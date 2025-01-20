package org.poo.services;

/**
 * Factory class for creating ServicePlanStrategy objects.
 */
public final class ServicePlanFactory {

    // Private constructor to prevent instantiation
    private ServicePlanFactory() {
        throw new UnsupportedOperationException("ServicePlanFactory cannot be instantiated");
    }

    /**
     * Creates a ServicePlanStrategy based on the given type.
     * @param planType the type of the plan ("silver", "gold", etc.)
     * @return a ServicePlanStrategy instance corresponding to the given type.
     */
    public static ServicePlanStrategy createServicePlan(final String planType) {
        return switch (planType.toLowerCase()) {
            case "silver" -> new SilverPlan();
            case "gold" -> new GoldPlan();
            case "student" -> new StudentPlan();
            default -> new StandardPlan();
        };
    }
}
