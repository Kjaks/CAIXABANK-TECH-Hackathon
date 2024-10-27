package com.hackathon.bankingapp.DTO;

import java.math.BigDecimal;
import java.util.HashMap;

public class ApiResponse extends HashMap<String, BigDecimal>{
    private String symbol;
    private BigDecimal valor;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
}
