package com.srqingchen.fieldworks.api.carbon;

/**
 * 碳排放精确申报接口(§5.1)：本模组机器与第三方机器可实现，
 * 引擎按配方执行周期拉取申报值。
 */
public interface ICarbonReporter {
    /** 每次配方执行的碳申报量(正=排放，负=封存)。 */
    double carbonPerOperation(String recipeId);
}
