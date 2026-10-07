package br.com.davi.homedavi.finance.adapter.out.integration.persistence.entity;

public enum HermesOutboxStatus {
  PENDING,
  SENDING,
  SENT,
  FAILED
}
