package io.tapdata.pdk.cli;

import picocli.CommandLine;

/**
 * Picocli aims to be the easiest way to create rich command line applications that can run on and off the JVM. Considering picocli? Check what happy users say about picocli.
 * https://picocli.info/
 *
 * @author aplomb
 */
public class MongoToMongoMain {
    //
    public static void main(String... args) {
//        String rootPath = "B:\\code\\tapdata\\idaas-pdk\\tapdata-pdk-cli\\src\\main\\resources\\stories\\";
        String rootPath = "tapdata-pdk-cli/src/main/resources/stories/";
        args = new String[]{"start",
                "-v",
//                rootPath + "emptyToFile.json",
//                rootPath + "emptyToAerospike.json",
//                rootPath + "tddToAerospike.json",
//                rootPath + "tddToEmpty.json",
                rootPath + "mongodbToMongodb.json",
//                rootPath + "tddToDoris.json",
//                rootPath + "vikaToVika.json",
        };
        Main.registerCommands().parseWithHandler(new CommandLine.RunLast(), args);
    }

}
