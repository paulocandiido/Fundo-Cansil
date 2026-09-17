package com.curso.resources.exceptions;

import com.curso.services.exceptions.ObjectNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.curso.cotacao.CotacaoIndisponivelException;
import com.curso.services.exceptions.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ControllerAdvice
public class ResourceExceptionHandler {
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<StandardError> jsonInvalido(org.springframework.http.converter.HttpMessageNotReadableException ex,HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST,"Bad Request",new IllegalArgumentException("JSON inválido ou tipo de campo incompatível"),request);
    }
    private ResponseEntity<StandardError> erro(HttpStatus status, String titulo, Exception ex, HttpServletRequest request) { return ResponseEntity.status(status).body(new StandardError(System.currentTimeMillis(), status.value(), titulo, ex.getMessage(), request.getRequestURI())); }

    @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<StandardError> validacao(MethodArgumentNotValidException ex,HttpServletRequest r){return erro(HttpStatus.BAD_REQUEST,"Validation error",new IllegalArgumentException("Dados de entrada inválidos"),r);}
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<StandardError> argumento(IllegalArgumentException ex,HttpServletRequest r){return erro(HttpStatus.BAD_REQUEST,"Bad Request",ex,r);}
    @ExceptionHandler(ConflitoException.class) public ResponseEntity<StandardError> conflito(ConflitoException ex,HttpServletRequest r){return erro(HttpStatus.CONFLICT,"Conflict",ex,r);}
    @ExceptionHandler(RegraNegocioException.class) public ResponseEntity<StandardError> regra(RegraNegocioException ex,HttpServletRequest r){return erro(HttpStatus.UNPROCESSABLE_ENTITY,"Business rule",ex,r);}
    @ExceptionHandler(BadCredentialsException.class) public ResponseEntity<StandardError> credenciais(BadCredentialsException ex,HttpServletRequest r){return erro(HttpStatus.UNAUTHORIZED,"Unauthorized",ex,r);}
    @ExceptionHandler(CotacaoIndisponivelException.class) public ResponseEntity<StandardError> cotacao(CotacaoIndisponivelException ex,HttpServletRequest r){return erro(HttpStatus.SERVICE_UNAVAILABLE,"Quote unavailable",ex,r);}
    @ExceptionHandler(CatalogoIndisponivelException.class) public ResponseEntity<StandardError> catalogo(CatalogoIndisponivelException ex,HttpServletRequest r){return erro(HttpStatus.SERVICE_UNAVAILABLE,"Asset catalog unavailable",ex,r);}
    @ExceptionHandler(CadastroIndisponivelException.class) public ResponseEntity<StandardError> cadastro(CadastroIndisponivelException ex,HttpServletRequest r){return erro(HttpStatus.SERVICE_UNAVAILABLE,"Company registry unavailable",ex,r);}

    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<StandardError> handleObjectNotFound(ObjectNotFoundException ex, HttpServletRequest request) {
        StandardError error = new StandardError(
                System.currentTimeMillis(),
                HttpStatus.NOT_FOUND.value(),
                "Object not found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<StandardError> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request)
    {

        StandardError error = new StandardError(System.currentTimeMillis(), HttpStatus.BAD_REQUEST.value(),
                "Bad Request", ex.getMessage(),request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StandardError> dataIntegrityViolationException(DataIntegrityViolationException ex, HttpServletRequest request){

        StandardError error = new StandardError(System.currentTimeMillis(),HttpStatus.BAD_REQUEST.value(),"Data Integrity Violation",
                ex instanceof GrupoComProdutosException ? ex.getMessage() : "Operação viola uma restrição de integridade",request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

}
