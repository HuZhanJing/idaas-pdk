package io.tapdata.pdk.cli.commands;

import com.google.common.collect.Multiset;
import io.tapdata.entity.codec.TapCodecsRegistry;
import io.tapdata.entity.codec.filter.TapCodecsFilterManager;
import io.tapdata.entity.conversion.TableFieldTypesGenerator;
import io.tapdata.entity.conversion.TargetTypesGenerator;
import io.tapdata.entity.mapping.DefaultExpressionMatchingMap;
import io.tapdata.entity.mapping.TapIterator;
import io.tapdata.entity.mapping.type.*;
import io.tapdata.entity.result.TapResult;
import io.tapdata.entity.schema.TapField;
import io.tapdata.entity.schema.TapTable;
import io.tapdata.entity.schema.type.*;
import io.tapdata.entity.simplify.pretty.BiClassHandlers;
import io.tapdata.entity.utils.DataMap;
import io.tapdata.entity.utils.InstanceFactory;
import io.tapdata.entity.utils.cache.KVMap;
import io.tapdata.entity.utils.cache.KVMapFactory;
import io.tapdata.pdk.apis.annotations.TapConnectorClass;
import io.tapdata.pdk.apis.functions.ConnectorFunctions;
import io.tapdata.pdk.apis.spec.TapNodeSpecification;
import io.tapdata.pdk.cli.CommonCli;
import io.tapdata.pdk.core.api.ConnectorNode;
import io.tapdata.pdk.core.api.PDKIntegration;
import io.tapdata.pdk.core.connector.TapConnector;
import io.tapdata.pdk.core.connector.TapConnectorManager;
import io.tapdata.pdk.core.tapnode.TapNodeInfo;
import io.tapdata.pdk.core.utils.CommonUtils;
import io.tapdata.pdk.tdd.core.PDKTestBase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import picocli.CommandLine;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.tapdata.entity.simplify.TapSimplify.field;
import static io.tapdata.entity.simplify.TapSimplify.table;

@CommandLine.Command(
        description = "Model prediction method",
        subcommands = MainCli.class
)
public class ModelPredictionCli extends CommonCli {
    private static final String TAG = ModelPredictionCli.class.getSimpleName();

    @CommandLine.Parameters(paramLabel = "FILE", description = "One ore more pdk jar files")
    File[] files;

    @CommandLine.Option(names = { "-o", "--output" }, required = true, description = "Specify the folder where model deduction report files will be generated")
    private String output;
    @CommandLine.Option(names = { "-m", "--mavenHome" }, required = true, description = "Specify the maven home")
    private String mavenHome;
    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "TapData cli help")
    private boolean helpRequested = false;

    private static final String FIELD_PREFIX = "prefix_";
    private static final String FIELD_EXACT = "exact_";

    public Integer execute() throws Exception {
        File outputFile = new File(output);
        if(outputFile.isFile())
            throw new IllegalArgumentException("");
        if(!outputFile.exists())
            FileUtils.forceMkdir(outputFile);
        if(!output.endsWith(File.separator)) {
            output = output + File.separator;
        }
        List<File> jarFiles = new ArrayList<>();

        initBiClassHandlers();

        for(File file : files) {
            String jarFile = null;
            if(file.isFile()) {
                jarFile = file.getAbsolutePath();
            } else if(file.isDirectory()) {
                if(!file.getAbsolutePath().contains("connectors")) {
                    throw new IllegalArgumentException("Connector project is under connectors directory, are you passing the correct connector project directory? " + file.getAbsolutePath());
                }

                if(mavenHome != null) {
                    System.setProperty("maven.home", mavenHome);
                }
                String pomFile = file.getAbsolutePath();
                if(!pomFile.endsWith("pom.xml")) {
                    pomFile = pomFile + File.separator + "pom.xml";
                }
//                    int state = mavenCli.doMain(new String[]{"install", "-f", pomFile}, "./", System.out, System.out);
                System.out.println(file.getName() + " is packaging...");
                InvocationRequest request = new DefaultInvocationRequest();
                request.setPomFile( new File( pomFile ) );
                request.setGoals( Collections.singletonList( "package" ) );

                Invoker invoker = new DefaultInvoker();
                InvocationResult result = invoker.execute( request );

                if ( result.getExitCode() != 0 )
                {
                    if(result.getExecutionException() != null)
                        System.out.println(result.getExecutionException().getMessage());
                    System.out.println("------------- Project " + pomFile + " package Failed --------------");
                    System.exit(0);
                } else {
                    System.out.println("------------- Project " + pomFile + " package successfully -------------");
                    MavenXpp3Reader reader = new MavenXpp3Reader();
                    Model model = reader.read(new FileReader(FilenameUtils.concat(file.getAbsolutePath(), "pom.xml")));
                    jarFile = FilenameUtils.concat("./", "./dist/" + model.getArtifactId() + "-v" + model.getVersion() + ".jar");
                }
            } else {
                throw new IllegalArgumentException("File " + file.getAbsolutePath() + " is not exist");
            }
            File theJarFile = new File(jarFile);
            if(!theJarFile.exists()) {
                throw new IllegalArgumentException("Packaged jar file " + jarFile + " not exists");
            }
            jarFiles.add(theJarFile);
//            CommonUtils.setProperty("pdk_test_jar_file", jarFile);
//            CommonUtils.setProperty("pdk_test_config_file", testConfig);
        }
        TapConnectorManager.getInstance().start(jarFiles);
        List<ConnectorNode> connectorNodes = new ArrayList<>();
        for(File file : jarFiles) {
            TapConnector testConnector = TapConnectorManager.getInstance().getTapConnectorByJarName(file.getName());
            Collection<TapNodeInfo> tapNodeInfoCollection = testConnector.getTapNodeClassFactory().getConnectorTapNodeInfos();
            for (TapNodeInfo nodeInfo : tapNodeInfoCollection) {
                TapNodeSpecification specification = nodeInfo.getTapNodeSpecification();
                String dagId = UUID.randomUUID().toString();
                KVMap<TapTable> kvMap = InstanceFactory.instance(KVMapFactory.class).getCacheMap(dagId, TapTable.class);
                KVMap<Object> stateMap = InstanceFactory.instance(KVMapFactory.class).getCacheMap(dagId, Object.class);
                ConnectorNode node = PDKIntegration.createConnectorBuilder()
                        .withDagId(dagId)
                        .withAssociateId("test")
                        .withGroup(specification.getGroup())
                        .withPdkId(specification.getId())
                        .withVersion(specification.getVersion())
                        .withTableMap(kvMap)
                        .withStateMap(stateMap)
                        .build();
                node.registerCapabilities();
                connectorNodes.add(node);
            }
        }

        for(ConnectorNode sourceNode : connectorNodes) {
            TapTable generatedTable = generateAllTypesTable(sourceNode);
            if(generatedTable == null)
                throw new NullPointerException("Generate all types for source " + sourceNode + " failed");
            for(ConnectorNode targetNode : connectorNodes) {
                if(!sourceNode.equals(targetNode)) {
                    TapResult<LinkedHashMap<String, TapField>> result = InstanceFactory.instance(TargetTypesGenerator.class).convert(generatedTable.getNameFieldMap(), targetNode.getConnectorContext().getSpecification().getDataTypesMap(), targetNode.getCodecsFilterManager());
                    if(result != null && result.getData() != null) {
                        outputCSVReport(sourceNode, generatedTable.getNameFieldMap(), targetNode, result.getData());
                    }
                }
            }
        }
        writeWorkbook();
        return 0;
    }

    private void writeWorkbook() {
        if(workbook == null)
            return;
        try
        {
            //Write the workbook in file system
            FileOutputStream out = FileUtils.openOutputStream(new File(output + "report_" + dateTime() + ".xlsx"));
            workbook.write(out);
            out.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private String dateTime() {
        return DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SSS").withZone(ZoneId.systemDefault()).format(new Date().toInstant());
    }

    XSSFWorkbook workbook;
    private void outputCSVReport(ConnectorNode sourceNode, LinkedHashMap<String, TapField> sourceNameFieldMap, ConnectorNode targetNode, LinkedHashMap<String, TapField> targetNameFieldMap) {
        //Blank workbook
        if(workbook == null)
            workbook = new XSSFWorkbook();
        CreationHelper creationHelper = (XSSFCreationHelper) workbook.getCreationHelper();

        //Create a blank sheet
        XSSFSheet sheet = workbook.createSheet(sourceNode.getConnectorContext().getSpecification().getName() + " -> " + targetNode.getConnectorContext().getSpecification().getName());
        //This data needs to be written (Object[])
        Map<String, Object[]> data = new LinkedHashMap<>();
        data.put("1", new Object[] {"No.", "Source type", "Source model", "Target type", "Target model"});
        Set<Map.Entry<String, TapField>> entries = sourceNameFieldMap.entrySet();
        int index = 0;
        for(Map.Entry<String, TapField> entry : entries) {
            TapField targetField = targetNameFieldMap.get(entry.getKey());
            data.put(String.valueOf(index + 2), new Object[] {index + 1,
                    entry.getValue().getDataType(), entry.getValue().getDataType(),
                    targetField.getDataType(), targetField.getDataType()
            });
            index++;
        }
        int sourceCommendIndex = 1;

        //https://www.baeldung.com/apache-poi-change-cell-font
        //Iterate over data and write to sheet
        Set<String> keyset = data.keySet();
        int rownum = 0;
        for (String key : keyset)
        {
            Row row = sheet.createRow(rownum++);
            Object [] objArr = data.get(key);
            int cellnum = 0;
            for (Object obj : objArr)
            {
                Cell cell = row.createCell(cellnum++);
                sheet.autoSizeColumn(cellnum - 1);
                if(sourceCommendIndex == cellnum - 1) {
                    Drawing drawing = sheet.createDrawingPatriarch();
                    ClientAnchor clientAnchor = drawing.createAnchor(0, 0, 0, 0, 0, 2, 7, 12);

                    Comment comment = drawing.createCellComment(clientAnchor);

                    RichTextString richTextString = creationHelper.createRichTextString(
                            "We can put a long comment here with \n a new line text followed by another \n new line text");

                    comment.setString(richTextString);
                    comment.setAuthor("Aplomb");

                    cell.setCellComment(comment);
                }


                CellStyle cellStyle = workbook.createCellStyle();

                cell.setCellStyle(cellStyle);
                if(obj instanceof String)
                    cell.setCellValue((String)obj);
                else if(obj instanceof Integer)
                    cell.setCellValue((Integer)obj);
            }
        }

    }

    private BiClassHandlers<TapMapping, TableExpressionWrapper, Void> biClassHandlers;
    private void initBiClassHandlers() {
        if(biClassHandlers == null) {
            biClassHandlers = new BiClassHandlers<>();
            biClassHandlers.register(TapStringMapping.class, this::handleStringMapping);
            biClassHandlers.register(TapYearMapping.class, this::handleYearMapping);
            biClassHandlers.register(TapNumberMapping.class, this::handleNumberMapping);
            biClassHandlers.register(TapRawMapping.class, this::handleRawMapping);
            biClassHandlers.register(TapBooleanMapping.class, this::handleBooleanMapping);
            biClassHandlers.register(TapDateMapping.class, this::handleDateMapping);
            biClassHandlers.register(TapDateTimeMapping.class, this::handleDateTimeMapping);
            biClassHandlers.register(TapTimeMapping.class, this::handleTimeMapping);
            biClassHandlers.register(TapMapMapping.class, this::handleMapMapping);
            biClassHandlers.register(TapArrayMapping.class, this::handleArrayMapping);
            biClassHandlers.register(TapBinaryMapping.class, this::handleBinaryMapping);
        }
    }

    private Void handleBinaryMapping(TapMapping tapMapping, TableExpressionWrapper tableExpressionWrapper) {
        HashSet<String> fields = new HashSet<>();
        TapBinaryMapping binaryMapping = (TapBinaryMapping) tapMapping;
        boolean needEmpty = true;
        if(binaryMapping.getBytes() != null) {
            needEmpty = false;
            TapResult<String> result;
            result = binaryMapping.fromTapType(tableExpressionWrapper.expression, new TapBinary().bytes(binaryMapping.getBytes()));
            if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                fields.add(result.getData());

            result = binaryMapping.fromTapType(tableExpressionWrapper.expression, new TapBinary().bytes(1L));
            if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                fields.add(result.getData());
        }
        if(needEmpty) {
            TapResult<String> result = binaryMapping.fromTapType(tableExpressionWrapper.expression, new TapBinary());
            if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                fields.add(result.getData());
        }

        for(String field : fields) {
            tableExpressionWrapper.table.add(field(FIELD_PREFIX + field, field));
        }
        return null;
    }

    private Void handleArrayMapping(TapMapping tapMapping, TableExpressionWrapper tableExpressionWrapper) {
        TapArrayMapping arrayMapping = (TapArrayMapping) tapMapping;
        TapResult<String> result = arrayMapping.fromTapType(tableExpressionWrapper.expression, new TapArray());
        String dataType = result.getData();
        tableExpressionWrapper.table.add(field(FIELD_PREFIX + dataType, dataType));
        return null;
    }

    private Void handleMapMapping(TapMapping tapMapping, TableExpressionWrapper tableExpressionWrapper) {
        TapMapMapping tapMapMapping = (TapMapMapping) tapMapping;
        TapResult<String> result = tapMapMapping.fromTapType(tableExpressionWrapper.expression, new TapMap());
        String dataType = result.getData();
        tableExpressionWrapper.table.add(field(FIELD_PREFIX + dataType, dataType));
        return null;
    }

    private Void handleTimeMapping(TapMapping tapMapping, TableExpressionWrapper tableExpressionWrapper) {
        TapTimeMapping tapMapMapping = (TapTimeMapping) tapMapping;
        TapResult<String> result = tapMapMapping.fromTapType(tableExpressionWrapper.expression, new TapTime());
        String dataType = result.getData();
        tableExpressionWrapper.table.add(field(FIELD_PREFIX + dataType, dataType));
        return null;
    }

    private Void handleDateTimeMapping(TapMapping tapMapping, TableExpressionWrapper tableExpressionWrapper) {
        Set<String> fieldDataTypes = new HashSet<>();
        TapDateTimeMapping dateTimeMapping = (TapDateTimeMapping) tapMapping;

        boolean needEmpty = true;
        if(dateTimeMapping.getMinFraction() != null && dateTimeMapping.getMaxFraction() != null) {
            needEmpty = false;
            TapResult<String> result;

            result = dateTimeMapping.fromTapType(tableExpressionWrapper.expression, new TapDateTime().fraction(dateTimeMapping.getMinFraction()));
            fieldDataTypes.add(result.getData());

            result = dateTimeMapping.fromTapType(tableExpressionWrapper.expression, new TapDateTime().fraction(dateTimeMapping.getMaxFraction()));
            fieldDataTypes.add(result.getData());
        }

        if(dateTimeMapping.getWithTimeZone() != null && dateTimeMapping.getWithTimeZone()) {
            TapResult<String> result;

            result = dateTimeMapping.fromTapType(tableExpressionWrapper.expression, new TapDateTime().withTimeZone(true));
            fieldDataTypes.add(result.getData());
        }

        if(needEmpty) {
            TapResult<String> result = dateTimeMapping.fromTapType(tableExpressionWrapper.expression, new TapDateTime());
            fieldDataTypes.add(result.getData());
        }

        for(String fieldDataType : fieldDataTypes) {
            tableExpressionWrapper.table.add(field(FIELD_PREFIX + fieldDataType, fieldDataType));
        }
        return null;
    }

    private Void handleDateMapping(TapMapping tapMapping, TableExpressionWrapper tableExpressionWrapper) {
        Set<String> fieldDataTypes = new HashSet<>();
        TapDateMapping dateMapping = (TapDateMapping) tapMapping;

        if(dateMapping.getWithTimeZone() != null && dateMapping.getWithTimeZone()) {
            TapResult<String> result;

            result = dateMapping.fromTapType(tableExpressionWrapper.expression, new TapDate().withTimeZone(true));
            fieldDataTypes.add(result.getData());

            result = dateMapping.fromTapType(tableExpressionWrapper.expression, new TapDate().withTimeZone(false));
            fieldDataTypes.add(result.getData());
        }
        TapResult<String> result = dateMapping.fromTapType(tableExpressionWrapper.expression, new TapDate());
        fieldDataTypes.add(result.getData());


        for(String fieldDataType : fieldDataTypes) {
            tableExpressionWrapper.table.add(field(FIELD_PREFIX + fieldDataType, fieldDataType));
        }

        return null;
    }

    private Void handleBooleanMapping(TapMapping tapMapping, TableExpressionWrapper tableExpressionWrapper) {
        TapBooleanMapping rawMapping = (TapBooleanMapping) tapMapping;
        TapResult<String> result = rawMapping.fromTapType(tableExpressionWrapper.expression, new TapBoolean());
        String dataType = result.getData();
        tableExpressionWrapper.table.add(field(FIELD_PREFIX + dataType, dataType));
        return null;
    }

    private Void handleRawMapping(TapMapping tapMapping, TableExpressionWrapper tableExpressionWrapper) {
        TapRawMapping rawMapping = (TapRawMapping) tapMapping;
        TapResult<String> result = rawMapping.fromTapType(tableExpressionWrapper.expression, new TapRaw());
        String dataType = result.getData();
        tableExpressionWrapper.table.add(field(FIELD_PREFIX + dataType, dataType));
        return null;
    }

    private Void handleNumberMapping(TapMapping tapMapping, TableExpressionWrapper tableExpressionWrapper) {
        HashSet<String> fields = new HashSet<>();
        TapNumberMapping numberMapping = (TapNumberMapping) tapMapping;
        boolean needEmpty = true;
        if(numberMapping.getBit() != null) {
            needEmpty = false;
            TapResult<String> result;
            result = numberMapping.fromTapType(tableExpressionWrapper.expression, new TapNumber().bit(numberMapping.getBit()));
            if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                fields.add(result.getData());
            result = numberMapping.fromTapType(tableExpressionWrapper.expression, new TapNumber().bit(1));
            if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                fields.add(result.getData());
            if(numberMapping.getUnsigned() != null) {
                result = numberMapping.fromTapType(tableExpressionWrapper.expression, new TapNumber().bit(numberMapping.getBit()).unsigned(true));
                if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                    fields.add(result.getData());

                result = numberMapping.fromTapType(tableExpressionWrapper.expression, new TapNumber().bit(numberMapping.getBit()).unsigned(false));
                if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                    fields.add(result.getData());
            }
        }
        if(numberMapping.getMaxPrecision() != null &&
                numberMapping.getMinPrecision() != null &&
                numberMapping.getMaxScale() != null &&
                numberMapping.getMinScale() != null
        ) {
            needEmpty = false;
            TapResult<String> result;
            result = numberMapping.fromTapType(tableExpressionWrapper.expression, new TapNumber().precision(numberMapping.getMaxPrecision()).scale(numberMapping.getMaxScale()));
            if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                fields.add(result.getData());
            result = numberMapping.fromTapType(tableExpressionWrapper.expression, new TapNumber().precision(numberMapping.getMaxPrecision()).scale(numberMapping.getMinScale()));
            if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                fields.add(result.getData());
            result = numberMapping.fromTapType(tableExpressionWrapper.expression, new TapNumber().precision(numberMapping.getMinPrecision()).scale(numberMapping.getMaxScale()));
            if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                fields.add(result.getData());
            result = numberMapping.fromTapType(tableExpressionWrapper.expression, new TapNumber().precision(numberMapping.getMinPrecision()).scale(numberMapping.getMinScale()));
            if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                fields.add(result.getData());
            if(numberMapping.getUnsigned() != null) {
                result = numberMapping.fromTapType(tableExpressionWrapper.expression, new TapNumber().precision(numberMapping.getMaxPrecision()).scale(numberMapping.getMaxScale()).unsigned(true));
                if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                    fields.add(result.getData());

                result = numberMapping.fromTapType(tableExpressionWrapper.expression, new TapNumber().precision(numberMapping.getMaxPrecision()).scale(numberMapping.getMaxScale()).unsigned(false));
                if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                    fields.add(result.getData());
            }
        }
        if(needEmpty) {
            TapResult<String> result = numberMapping.fromTapType(tableExpressionWrapper.expression, new TapNumber());
            if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                fields.add(result.getData());
        }
        for(String field : fields) {
            tableExpressionWrapper.table.add(field(FIELD_PREFIX + field, field));
        }
        return null;
    }

    private Void handleYearMapping(TapMapping tapMapping, TableExpressionWrapper tableExpressionWrapper) {
        TapYearMapping yearMapping = (TapYearMapping) tapMapping;
        TapResult<String> result = yearMapping.fromTapType(tableExpressionWrapper.expression, new TapYear());
        String dataType = result.getData();
        tableExpressionWrapper.table.add(field(FIELD_PREFIX + dataType, dataType));
        return null;
    }

    private Void handleStringMapping(TapMapping tapMapping, TableExpressionWrapper tableExpressionWrapper) {
        HashSet<String> fieldDataTypes = new HashSet<>();
        TapStringMapping stringMapping = (TapStringMapping) tapMapping;
        if(stringMapping.getBytes() != null) {
            TapResult<String> result;
            result = stringMapping.fromTapType(tableExpressionWrapper.expression, new TapString().bytes(stringMapping.actualBytes()).byteRatio(stringMapping.getByteRatio()));
            if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                fieldDataTypes.add(result.getData());

            result = stringMapping.fromTapType(tableExpressionWrapper.expression, new TapString().bytes(1L * stringMapping.getByteRatio()).byteRatio(stringMapping.getByteRatio()));
            if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                fieldDataTypes.add(result.getData());
        } else {
            TapResult<String> result = stringMapping.fromTapType(tableExpressionWrapper.expression, new TapString());
            if(result != null && result.getResult() != TapResult.RESULT_FAILED)
                fieldDataTypes.add(result.getData());
        }
        for(String fieldDataType : fieldDataTypes) {
            tableExpressionWrapper.table.add(field(FIELD_PREFIX + fieldDataType, fieldDataType));
        }
        return null;
    }

    private TapTable generateAllTypesTable(ConnectorNode node) {
        final TapTable tapTable = table(node.getAssociateId());
        DefaultExpressionMatchingMap expressionMatchingMap = node.getConnectorContext().getSpecification().getDataTypesMap();
        expressionMatchingMap.iterate(expressionDataMapEntry ->  {
            String expression = expressionDataMapEntry.getKey();
            tapTable.add(field(FIELD_EXACT + expression, expression));
            return false;
        }, DefaultExpressionMatchingMap.ITERATE_TYPE_EXACTLY_ONLY);

        expressionMatchingMap.iterate(expressionDataMapEntry ->  {
            String expression = expressionDataMapEntry.getKey();
            DataMap dataMap = expressionDataMapEntry.getValue();
            TapMapping tapMapping = (TapMapping) dataMap.get(TapMapping.FIELD_TYPE_MAPPING);
//            tapTable.add(field(expression, tapMapping.toTapType()))
            fillTestFields(tapTable, expression, tapMapping);
            return false;
        }, DefaultExpressionMatchingMap.ITERATE_TYPE_PREFIX_ONLY);
        InstanceFactory.instance(TableFieldTypesGenerator.class).autoFill(tapTable.getNameFieldMap(), expressionMatchingMap);
        return tapTable;
    }

    private void fillTestFields(TapTable tapTable, String expression, TapMapping tapMapping) {
        biClassHandlers.handle(tapMapping, new TableExpressionWrapper(tapTable,  expression));
    }

    public class TableExpressionWrapper {
        public TableExpressionWrapper(TapTable table, String expression) {
            this.table = table;
            this.expression = expression;
        }
        private TapTable table;
        private String expression;
    }
}
