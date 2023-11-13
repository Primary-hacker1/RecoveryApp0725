package com.example.usbprint.print;

import com.dynamixsoftware.printingsdk.TransportType;

import java.io.Serializable;
import java.util.List;

public class DriversSearchEntryBean implements Serializable {

    private TransportType transportType;
    private List<mDriverHandle> listMDriverHandle;

    public TransportType getTransportType() {
        return transportType;
    }

    public List<mDriverHandle> getDriverHandlesList() {
        return listMDriverHandle;
    }

    public DriversSearchEntryBean(TransportType transportType, List<mDriverHandle> listMDriverHandle) {
        this.transportType = transportType;
        this.listMDriverHandle = listMDriverHandle;
    }

    public DriversSearchEntryBean() {
    }

    public void setTransportType(TransportType transportType) {
        this.transportType = transportType;
    }

    public List<mDriverHandle> getListDriverHandle() {
        return listMDriverHandle;
    }

    public void setListDriverHandle(List<mDriverHandle> listMDriverHandle) {
        this.listMDriverHandle = listMDriverHandle;
    }

    public static class mDriverHandle implements Serializable{
        private String id;
        private String printerName;
        private boolean isGeneric;

        public String getId() {
            return id;
        }

        public String getPrinterName() {
            return printerName;
        }

        public boolean isGeneric() {
            return isGeneric;
        }

        public mDriverHandle(String id, String printerName, boolean isGeneric) {
            this.id = id;
            this.printerName = printerName;
            this.isGeneric = isGeneric;
        }

        public mDriverHandle() {
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setPrinterName(String printerName) {
            this.printerName = printerName;
        }

        public void setGeneric(boolean generic) {
            isGeneric = generic;
        }
    }
}
