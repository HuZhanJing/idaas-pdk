package io.tapdata.entity.mapping.type;

import io.tapdata.entity.result.TapResult;
import io.tapdata.entity.schema.TapField;
import io.tapdata.entity.schema.type.TapDate;
import io.tapdata.entity.schema.type.TapType;

import java.math.BigDecimal;
import java.util.Map;

/**
 * "date": {"range": ["1000-01-01", "9999-12-31"], "gmt" : 0, "to": "typeDate"},
 */
public class TapDateMapping extends TapDateBase {

    @Override
    protected String pattern() {
        return "yyyy-MM-dd";
    }

    @Override
    public TapType toTapType(String dataType, Map<String, String> params) {
        return new TapDate();
    }

    @Override
    public TapResult<String> fromTapType(String typeExpression, TapType tapType) {
        if (tapType instanceof TapDate) {
            return TapResult.successfully(removeBracketVariables(typeExpression, 0));
        }
        return null;
    }

    @Override
    public BigDecimal matchingScore(TapField field) {
        if (field.getTapType() instanceof TapDate) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(-Double.MAX_VALUE);
    }
}
