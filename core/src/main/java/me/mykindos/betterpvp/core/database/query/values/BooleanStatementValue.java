package me.mykindos.betterpvp.core.database.query.values;

import me.mykindos.betterpvp.core.database.query.StatementValue;

import java.sql.Types;

public class BooleanStatementValue extends StatementValue<Boolean> {

    public BooleanStatementValue(Boolean value) {
        super(value);
    }

    @Override
    public int getType() {
        return Types.TINYINT;
    }

    public static BooleanStatementValue of(Boolean value) {
        return new BooleanStatementValue(value);
    }

}
