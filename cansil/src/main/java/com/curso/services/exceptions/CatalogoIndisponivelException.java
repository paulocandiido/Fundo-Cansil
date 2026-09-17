package com.curso.services.exceptions;
public class CatalogoIndisponivelException extends RuntimeException{public CatalogoIndisponivelException(){super("Catálogo de ativos indisponível no momento");}public CatalogoIndisponivelException(Throwable causa){super("Catálogo de ativos indisponível no momento",causa);}}
