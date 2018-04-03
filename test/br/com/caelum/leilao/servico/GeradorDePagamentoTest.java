package br.com.caelum.leilao.servico;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.*;
//import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Calendar;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.dominio.Pagamento;
import br.com.caelum.leilao.dominio.Usuario;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;
import br.com.caelum.leilao.infra.dao.RepositorioDePagamentos;
import br.com.caelum.leilao.infra.relogio.Relogio;

public class GeradorDePagamentoTest {

	@Test
	public void deveGerarPagamentoParaUmLeilaoEncerrado() {
		//Arrange
		RepositorioDeLeiloes leiloes = mock(RepositorioDeLeiloes.class);
		RepositorioDePagamentos pagamentos = mock(RepositorioDePagamentos.class);
//		Avaliador avaliador = mock(Avaliador.class);
		Avaliador avaliador = new Avaliador();
		
		Leilao leilao = new CriadorDeLeilao()
				.para("Playstation")
				.lance(new Usuario("José da Silva"), 2000.0)
				.lance(new Usuario("Maria Pereira"), 2500.0)
				.constroi();
		
		when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));
//		when(avaliador.getMaiorLance()).thenReturn(2500.0);
		
		//Act
		GeradorDePagamento gerador = new GeradorDePagamento(leiloes, pagamentos, avaliador);
		gerador.gera();
		
		//Assert
		ArgumentCaptor<Pagamento> argumento = ArgumentCaptor.forClass(Pagamento.class);
		verify(pagamentos).salva(argumento.capture());
		
		Pagamento pagamentoGerado = argumento.getValue();
		
		assertThat(pagamentoGerado.getValor(), is(2500.0));
	}

	
	@Test
	public void deveEmpurrarParaOProximoDiaUtilSePagamentoCairNoSabado() {
		//Arrange
		RepositorioDeLeiloes leiloes = mock(RepositorioDeLeiloes.class);
		RepositorioDePagamentos pagamentos = mock(RepositorioDePagamentos.class);
		Relogio relogio = mock(Relogio.class);
		
		Leilao leilao = new CriadorDeLeilao()
				.para("Playstation")
				.lance(new Usuario("José da Silva"), 2000.0)
				.lance(new Usuario("Maria Pereira"), 2500.0)
				.constroi();
		
		when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));
		
		Calendar sabado = Calendar.getInstance();
		sabado.set(2018, Calendar.FEBRUARY, 24);
		when(relogio.hoje()).thenReturn(sabado);
		
		//Act
		GeradorDePagamento gerador = new GeradorDePagamento(leiloes, pagamentos, new Avaliador(), relogio);
		gerador.gera();
		
		//Assert
		ArgumentCaptor<Pagamento> argCaptor = ArgumentCaptor.forClass(Pagamento.class);
		verify(pagamentos).salva(argCaptor.capture());
		
		Pagamento pagamentoGerado = argCaptor.getValue();
		
		assertEquals(Calendar.MONDAY, pagamentoGerado.getData().get(Calendar.DAY_OF_WEEK));
		assertEquals(26, pagamentoGerado.getData().get(Calendar.DAY_OF_MONTH));
	}
	
	@Test
	public void deveEmpurrarParaOProximoDiaUtilSePagamentoCairNoDomingo() {
		//Arrange
		RepositorioDeLeiloes leiloes = mock(RepositorioDeLeiloes.class);
		RepositorioDePagamentos pagamentos = mock(RepositorioDePagamentos.class);
		Relogio relogio = mock(Relogio.class);

		Leilao leilao = new CriadorDeLeilao()
				.para("Playstation")
				.lance(new Usuario("José da Silva"), 2000.0)
				.lance(new Usuario("Maria Pereira"), 2500.0)
				.constroi();

		when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));
		
		Calendar domingo = Calendar.getInstance();
		domingo.set(2018, Calendar.FEBRUARY, 25);
		when(relogio.hoje()).thenReturn(domingo);
		
		//Act
		GeradorDePagamento gerador = new GeradorDePagamento(leiloes, pagamentos, new Avaliador(), relogio);
		gerador.gera();
		
		//Assert
		ArgumentCaptor<Pagamento> argCaptor = ArgumentCaptor.forClass(Pagamento.class);
		verify(pagamentos).salva(argCaptor.capture());
		
		Pagamento pagamentoGerado = argCaptor.getValue();
		
		assertEquals(Calendar.MONDAY, pagamentoGerado.getData().get(Calendar.DAY_OF_WEEK));
		assertEquals(26, pagamentoGerado.getData().get(Calendar.DAY_OF_MONTH));
	}
}
