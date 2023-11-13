package com.example.usbprint.print;

import com.dynamixsoftware.printingsdk.PrinterContext;
import com.dynamixsoftware.printingsdk.PrinterOption;
import com.dynamixsoftware.printingsdk.TransportType;

import java.util.List;

public class PrinterBean {
    private int type;
    private String name;
    private String owner;
    private String descr;
    private List<String> id;
    private PrinterContext context;
    private List<PrinterOption> printerOptions;
    private List<TransportType> transportTypes;

    public PrinterBean(int type, String name, String owner, String descr, List<String> id,
                       PrinterContext context, List<PrinterOption> printerOptions, List<TransportType> transportTypes) {
        this.type = type;
        this.name = name;
        this.owner = owner;
        this.descr = descr;
        this.id = id;
        this.context = context;
        this.printerOptions = printerOptions;
        this.transportTypes = transportTypes;
    }

    public PrinterBean() {}

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public List<String> getId() {
        return id;
    }

    public void setId(List<String> id) {
        this.id = id;
    }

    public PrinterContext getContext() {
        return context;
    }

    public void setContext(PrinterContext context) {
        this.context = context;
    }

    public List<PrinterOption> getPrinterOptions() {
        return printerOptions;
    }

    public void setPrinterOptions(List<PrinterOption> printerOptions) {
        this.printerOptions = printerOptions;
    }

    public List<TransportType> getTransportTypes() {
        return transportTypes;
    }

    public void setTransportTypes(List<TransportType> transportTypes) {
        this.transportTypes = transportTypes;
    }
}
