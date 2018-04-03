package br.com.caelum.leilao.servico;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import static org.mockito.Mockito.*;
import static org.mockito.Matchers.*;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;

import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;
import br.com.caelum.leilao.infra.email.EnviadorDeEmail;

public class EncerradorDeLeilaoTest {

	private Leilao leilao1;
	private Leilao leilao2;
	private RepositorioDeLeiloes mockDao;
	private EnviadorDeEmail mockEnviadorDeEmail;

	@Before
	public void setup() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 3, 12);
		
		leilao1 = new CriadorDeLeilao().para("TV de Plasma").naData(antiga).constroi();
		leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

		mockDao = mock(RepositorioDeLeiloes.class);
		mockEnviadorDeEmail = mock(EnviadorDeEmail.class);
	}
	
	@Test
	public void deveEncerrarLeiloesQueComeçaramHaMaisDeUmaSemanaAtras() {
		//Arrange
		
		List<Leilao> leiloesAntigos = Arrays.asList(leilao1, leilao2);
		
		when(mockDao.correntes()).thenReturn(leiloesAntigos);
		
		
		//Act
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(mockDao, mockEnviadorDeEmail);
		encerrador.encerra();
		
		//Assert
		assertThat(encerrador.getTotalEncerrados(), is(2));
		assertThat(leilao1.isEncerrado(), is(true));
		assertThat(leilao2.isEncerrado(), is(true));
		
		verify(mockDao).atualiza(leilao1);
		verify(mockDao).atualiza(leilao2);
		
		verify(mockEnviadorDeEmail).envia(leilao1);
		verify(mockEnviadorDeEmail).envia(leilao2);
	}
	
	@Test
	public void naoDeveEncerrarLeilaoComecadoOntem() {
		//Arrange
		Calendar ontem = Calendar.getInstance();
		ontem.set(2018, 1, 14);
		
		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(ontem).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(ontem).constroi();
		
		when(mockDao.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		
		//Act
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(mockDao, mockEnviadorDeEmail);
		encerrador.encerra();
		
		//Assert
		assertThat(encerrador.getTotalEncerrados(), is(0));
		assertThat(leilao1.isEncerrado(), is(false));
		assertThat(leilao2.isEncerrado(), is(false));
		
		verify(mockDao, never()).atualiza(leilao1);
		verify(mockDao, times(0)).atualiza(leilao2);
		
		verify(mockEnviadorDeEmail, never()).envia(leilao1);
		verify(mockEnviadorDeEmail, never()).envia(leilao2);

	}
	
	@Test
	public void deveReceberListaVaziaENaoFazerNada() {
		//Arrange
		when(mockDao.correntes()).thenReturn(new ArrayList<Leilao>());
		
		//Act
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(mockDao, mockEnviadorDeEmail);
		encerrador.encerra();
		
		//Assert
		assertThat(encerrador.getTotalEncerrados(), is(0));
	}
	
	@Test
	public void deveAtualizarLeiloesEncerrados() {
		//Arrange
		when(mockDao.correntes()).thenReturn(Arrays.asList(leilao1));
		
		//Act
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(mockDao, mockEnviadorDeEmail);
		encerrador.encerra();
		
		//Assert
		
		InOrder inOrder = inOrder(mockDao, mockEnviadorDeEmail);
		
		//primeira invocação
		inOrder.verify(mockDao, times(1)).atualiza(leilao1);
		
		//segunda invocação
		inOrder.verify(mockEnviadorDeEmail, times(1)).envia(leilao1);
	}

	@Test
	public void deveContinuarAExecucaoMesmoQuandoDaoFalha() {
		//Arrange
		when(mockDao.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		
		doThrow(new RuntimeException()).when(mockDao).atualiza(leilao1);
		
		//Act
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(mockDao, mockEnviadorDeEmail);
		encerrador.encerra();
				
		//Assert
		verify(mockEnviadorDeEmail, never()).envia(leilao1);
		verify(mockDao).atualiza(leilao2);
		verify(mockEnviadorDeEmail).envia(leilao2);
	}
	
	@Test
	public void deveContinuarAExecucaoMesmoQuandoEmailFalha() {
		//Arrange
		
		when(mockDao.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

		doThrow(new RuntimeException()).when(mockEnviadorDeEmail).envia(leilao1);
		
		//Act
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(mockDao, mockEnviadorDeEmail);
		encerrador.encerra();
		
		//Assert
		verify(mockDao).atualiza(leilao1);
		verify(mockDao).atualiza(leilao2);
		verify(mockEnviadorDeEmail).envia(leilao2);
	}
	
	@Test
	public void naoDeveEnviarEmailQuandoDaoFalhaParaTodosOsLeiloes() {
		//Arrange
		when(mockDao.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		
//		doThrow(new RuntimeException()).when(mockDao).atualiza(leilao1);
//		doThrow(new RuntimeException()).when(mockDao).atualiza(leilao2);

		doThrow(new RuntimeException()).when(mockDao).atualiza(any(Leilao.class));
		
		//Act
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(mockDao, mockEnviadorDeEmail);
		encerrador.encerra();
		
		//Assert
//		verify(mockEnviadorDeEmail, never()).envia(leilao1);
//		verify(mockEnviadorDeEmail, never()).envia(leilao2);
		verify(mockEnviadorDeEmail, never()).envia(any(Leilao.class));
	}
}
