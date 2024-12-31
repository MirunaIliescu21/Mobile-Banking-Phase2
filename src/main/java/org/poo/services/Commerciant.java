package org.poo.services;

public class Commerciant {
    private final String name;
    private final int id;
    private final String account;
    private final String type;
    private final String cashbackStrategy;

    public Commerciant(final String name, final int id, final String account,
                       final String type, final String cashbackStrategy) {
        this.name = name;
        this.id = id;
        this.account = account;
        this.type = type;
        this.cashbackStrategy = cashbackStrategy;
    }

    @Override
    public String toString() {
        return "Commerciant{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", account='" + account + '\'' +
                ", type='" + type + '\'' +
                ", cashbackStrategy='" + cashbackStrategy + '\'' +
                '}';
    }
}
