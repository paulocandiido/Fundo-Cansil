package com.curso.repositories;

import com.curso.cansil.CansilApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes=CansilApplication.class)
class AtivoConcurrencyTest {
    @Autowired AtivoRepository ativos;

    @Test void criacoesConcorrentesRetornamMesmoAtivo() throws Exception {
        var executor=Executors.newFixedThreadPool(2);
        var prontos=new CountDownLatch(2);
        var iniciar=new CountDownLatch(1);
        Callable<Long> criar=()->{
            assertTrue(ativos.findBySimboloIgnoreCase("CONC4").isEmpty());
            prontos.countDown();
            assertTrue(iniciar.await(10,TimeUnit.SECONDS));
            return ativos.obterOuCriar("CONC4","Concorrência","B3").getId();
        };
        try {
            var a=executor.submit(criar);
            var b=executor.submit(criar);
            assertTrue(prontos.await(10,TimeUnit.SECONDS));
            iniciar.countDown();
            assertEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }
    }
}
