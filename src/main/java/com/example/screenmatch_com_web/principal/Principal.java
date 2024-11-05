package com.example.screenmatch_com_web.principal;

import com.example.screenmatch_com_web.model.*;
import com.example.screenmatch_com_web.repository.SerieRepository;
import com.example.screenmatch_com_web.service.ConsumoApi;
import com.example.screenmatch_com_web.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;


public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";

    private SerieRepository repository;
    private List<Serie> seriesList = new ArrayList<>();
    private Optional<Serie> serieBusca;

    public Principal(SerieRepository repository) {
        this.repository = repository;
    }


    public void exibeMenu() {

        Integer opcao = -1;

        while (opcao != 0) {
            var menu = """
                1 - Buscar séries
                2 - Buscar episódios
                3 - Listar séries buscadas
                4 - Buscar série pelo título
                5 - Buscar séries por ator
                6 - Top 5 séries
                7 - Buscar por gênero
                8 - Buscar séries por qtd de Temporadas e avaliação
                9 - Buscar episódios por trecho
                10 - Top 5 episódios de uma série
                11 - Buscar episódios a partir de uma data
                
                0 - Sair                                 
                """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriesPorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 8:
                    filtrarSeriesPorTemporadaEavaliacao();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodioAposData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        repository.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.println("Digite uma série que deseja procurar: ");
        var nomeSerie = leitura.nextLine();
        Optional<Serie> serie = repository.findByTituloContainingIgnoreCase(nomeSerie);
        if (serie.isPresent()) {
            var serieEncontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();
            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO +
                        serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }

            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream().flatMap(f -> f.episodios().stream()
                            .map(e -> new Episodio(f.numero(), e)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);
            repository.save(serieEncontrada);
        } else {
            System.out.println("Série não encontrada");
        }
    }

    private void listarSeriesBuscadas() {
        seriesList = repository.findAll();
        seriesList.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();
        serieBusca = repository.findByTituloContainingIgnoreCase(nomeSerie);
        if (serieBusca.isPresent()) {
            System.out.println("Dados da série" + serieBusca.get());
        } else {
            System.out.println("Série não encontrada");
        }
    }

    private void buscarSeriesPorAtor() {
        System.out.println("Digite o nome do ator: ");
        var nomeProcurado = leitura.nextLine();
        System.out.println("Qual nota mínima as séries devem ter?");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEncontradas = repository.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeProcurado, avaliacao);
        System.out.println("Séries em que " + nomeProcurado + " trabalhou");
        seriesEncontradas.forEach(f ->
                System.out.println(f.getTitulo() + " avaliação: " + f.getAvaliacao()));
    }

    private void buscarTop5Series() {
        List<Serie> serieList = repository.findTop5ByOrderByAvaliacaoDesc();
        serieList.forEach(f ->
                System.out.println(f.getTitulo() + ", Avaliação: " + f.getAvaliacao()));
    }

    private void buscarSeriesPorCategoria() {

        System.out.println("Qual gênero deseja buscar?");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPoruguese(nomeGenero);
        List<Serie> serieList = repository.findByGenero(categoria);
        System.out.println("Séries da categoria " + nomeGenero);
        serieList.forEach(System.out::println);
    }

    private void filtrarSeriesPorTemporadaEavaliacao() {
        System.out.println("Filtrar séries até quantas temporadas?");
        var totalTemporadas = leitura.nextInt();
        leitura.nextLine();
        System.out.println("Com avaliação a partir de que valor?");
        var avaliacao = leitura.nextDouble();
        List<Serie> serieList =
                repository.seriesPorTemporadaEavaliacao(totalTemporadas, avaliacao);
        System.out.println("Séries filtradas: ");
        serieList.forEach(f -> System.out.println(f.getTitulo() + "\nAvaliação: " + f.getAvaliacao()
                + "\nTotal de Temporadas: " + f.getTotalTemporadas()));
    }

    private void buscarEpisodioPorTrecho() {
        System.out.println("Digite o trecho do nome do episódio: ");
        var episodioProcurado = leitura.nextLine();
        List<Episodio> episodioList = repository.episodiosPorTrecho(episodioProcurado);
        episodioList.forEach(e ->
                System.out.printf("Série: %s Temporada %s - Episódio %s - %s\n",
                        e.getSerie(), e.getTemporada(), e.getNumeroEpisodio(), e.getTitulo()));
    }

    private void topEpisodiosPorSerie() {
        buscarSeriePorTitulo();
        if(serieBusca.isPresent()) {
           Serie serie = serieBusca.get();
           List<Episodio> episodioList = repository.topEpisodiosPorSerie(serie);
           episodioList.forEach(e ->
                   System.out.printf("Série: %s Temporada %s - Episódio %s - Avaliação %s - %s\n",
                           e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(), e.getAvaliacao() ,e.getTitulo()));
        }

    }

    private void buscarEpisodioAposData() {
        buscarSeriePorTitulo();
        if(serieBusca.isPresent()) {
            Serie serie = serieBusca.get();
            System.out.println("Digite o ano limite de lançamento");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();

            List<Episodio> episodioList = repository.episodiosPorSerieAposData(serie, anoLancamento);
            episodioList.forEach(System.out::println);
        }
    }
}
