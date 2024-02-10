package com.kevinguanchedarias.owgejava.entity.dialect;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

public class OwgeMysqlFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry()
                .register("TIME_TO_SEC", new StandardSQLFunction("TIME_TO_SEC", StandardBasicTypes.DOUBLE));
    }
}
