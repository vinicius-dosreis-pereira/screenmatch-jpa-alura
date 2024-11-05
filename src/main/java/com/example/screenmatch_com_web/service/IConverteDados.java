package com.example.screenmatch_com_web.service;

public interface IConverteDados {
    <T> T  obterDados(String json, Class<T> classe);
}
