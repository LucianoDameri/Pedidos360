package cl.duoc.pedidos360.ms_kafka_admin.service;

import cl.duoc.pedidos360.ms_kafka_admin.config.KafkaAdminProperties;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.GroupDetail;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.TopicDetail;
import cl.duoc.pedidos360.ms_kafka_admin.dto.PublishRequest;
import cl.duoc.pedidos360.ms_kafka_admin.dto.TopicConfigRequest;
import cl.duoc.pedidos360.ms_kafka_admin.dto.TopicRequest;
import cl.duoc.pedidos360.ms_kafka_admin.exception.ConflictoException;
import cl.duoc.pedidos360.ms_kafka_admin.exception.KafkaNoDisponibleException;
import cl.duoc.pedidos360.ms_kafka_admin.exception.RecursoNoEncontradoException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaAdminServiceTest {

    @Mock
    private Admin admin;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaAdminService service;

    @BeforeEach
    void preparar() {
        KafkaAdminProperties props = new KafkaAdminProperties();
        props.setTimeoutMs(1000);
        props.setDefaultPartitions(3);
        props.setDefaultReplicationFactor((short) 2);
        service = new KafkaAdminService(admin, kafkaTemplate, props);
    }

    // ------------------------------------------------------------------ ayudas
    private TopicDescription descripcion(String nombre, int particiones) {
        Node n1 = new Node(1, "kafka-1", 29092);
        Node n2 = new Node(2, "kafka-2", 29092);
        List<TopicPartitionInfo> info = new java.util.ArrayList<>();
        for (int i = 0; i < particiones; i++) {
            info.add(new TopicPartitionInfo(i, n1, List.of(n1, n2), List.of(n1, n2)));
        }
        return new TopicDescription(nombre, false, info);
    }

    /** Deja listo describeTopics + describeConfigs para el topico indicado. */
    private void existeTopico(String nombre, int particiones) {
        DescribeTopicsResult describe = mock(DescribeTopicsResult.class);
        when(describe.allTopicNames()).thenReturn(KafkaFuture.completedFuture(Map.of(nombre, descripcion(nombre, particiones))));
        when(admin.describeTopics(anyCollection())).thenReturn(describe);

        DescribeConfigsResult configs = mock(DescribeConfigsResult.class);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = new Config(List.of(
                new ConfigEntry("retention.ms", "604800000"),
                new ConfigEntry("cleanup.policy", "delete"),
                new ConfigEntry("min.insync.replicas", "1")));
        when(configs.all()).thenReturn(KafkaFuture.completedFuture(Map.of(recurso, config)));
        when(admin.describeConfigs(anyCollection())).thenReturn(configs);
    }

    private void noExisteTopico() {
        DescribeTopicsResult describe = mock(DescribeTopicsResult.class);
        KafkaFutureImpl<Map<String, TopicDescription>> fallo = new KafkaFutureImpl<>();
        fallo.completeExceptionally(new UnknownTopicOrPartitionException("no existe"));
        when(describe.allTopicNames()).thenReturn(fallo);
        when(admin.describeTopics(anyCollection())).thenReturn(describe);
    }

    // ------------------------------------------------------------------ topicos
    @Test
    void crearUnTopicoSinParametrosUsaLosValoresPorDefectoDelServicio() {
        CreateTopicsResult creacion = mock(CreateTopicsResult.class);
        when(creacion.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.createTopics(anyCollection())).thenReturn(creacion);
        existeTopico("demo", 3);

        TopicRequest peticion = new TopicRequest();
        peticion.setName("demo");
        peticion.setRetentionMs(86400000L);
        peticion.setCleanupPolicy("compact,delete");
        TopicDetail detalle = service.crearTopico(peticion);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<NewTopic>> captura = ArgumentCaptor.forClass(Collection.class);
        verify(admin).createTopics(captura.capture());
        NewTopic nuevo = captura.getValue().iterator().next();
        assertEquals("demo", nuevo.name());
        assertEquals(3, nuevo.numPartitions());
        assertEquals(2, nuevo.replicationFactor());
        assertEquals("86400000", nuevo.configs().get("retention.ms"));
        assertEquals("compact,delete", nuevo.configs().get("cleanup.policy"));
        assertEquals("demo", detalle.name());
        assertEquals(3, detalle.partitions());
        assertEquals(2, detalle.replicationFactor());
    }

    @Test
    void noSePuedeCrearUnTopicoInternoDeKafka() {
        TopicRequest peticion = new TopicRequest();
        peticion.setName("__consumer_offsets");

        assertThrows(IllegalArgumentException.class, () -> service.crearTopico(peticion));
        verifyNoInteractions(admin);
    }

    @Test
    void minInsyncReplicasNoPuedeSerMayorQueLasReplicas() {
        TopicRequest peticion = new TopicRequest();
        peticion.setName("demo");
        peticion.setReplicationFactor((short) 2);
        peticion.setMinInsyncReplicas(3);

        assertThrows(IllegalArgumentException.class, () -> service.crearTopico(peticion));
        verifyNoInteractions(admin);
    }

    @Test
    void describirUnTopicoMuestraParticionesLiderIsrYConfiguracion() {
        existeTopico("pedidos.eventos", 3);

        TopicDetail detalle = service.describirTopico("pedidos.eventos");

        assertEquals(3, detalle.partitions());
        assertEquals(3, detalle.partitionInfo().size());
        assertEquals(1, detalle.partitionInfo().get(0).leader());
        assertEquals(List.of(1, 2), detalle.partitionInfo().get(0).isr());
        assertEquals("604800000", detalle.configs().get("retention.ms"));
        assertEquals("delete", detalle.configs().get("cleanup.policy"));
    }

    @Test
    void describirUnTopicoInexistenteEs404() {
        noExisteTopico();

        assertThrows(RecursoNoEncontradoException.class, () -> service.describirTopico("fantasma"));
    }

    @Test
    void eliminarUnTopicoInexistenteEs404YNoLlamaADelete() {
        noExisteTopico();

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminarTopico("fantasma"));
        verify(admin).describeTopics(anyCollection());
        verifyNoMoreInteractions(admin);
    }

    @Test
    void noSePuedeEliminarUnTopicoInterno() {
        assertThrows(IllegalArgumentException.class, () -> service.eliminarTopico("__consumer_offsets"));
        verifyNoInteractions(admin);
    }

    @Test
    void soloSePuedenAumentarLasParticiones() {
        existeTopico("pedidos.eventos", 3);

        assertThrows(IllegalArgumentException.class, () -> service.aumentarParticiones("pedidos.eventos", 3));
        assertThrows(IllegalArgumentException.class, () -> service.aumentarParticiones("pedidos.eventos", 2));
    }

    @Test
    void actualizarConfiguracionExigeAlMenosUnCampo() {
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarConfiguracion("pedidos.eventos", new TopicConfigRequest()));
        verifyNoInteractions(admin);
    }

    @Test
    void minInsyncReplicasNoPuedeSuperarElFactorDeReplicacionDelTopico() {
        existeTopico("pedidos.eventos", 3);
        TopicConfigRequest peticion = new TopicConfigRequest();
        peticion.setMinInsyncReplicas(5);

        assertThrows(IllegalArgumentException.class, () -> service.actualizarConfiguracion("pedidos.eventos", peticion));
    }

    @Test
    void construirConfigsSoloIncluyeLoQueVieneInformado() {
        Map<String, String> configs = KafkaAdminService.construirConfigs(604800000L, "delete", null, 1048576);

        assertEquals(3, configs.size());
        assertEquals("604800000", configs.get("retention.ms"));
        assertEquals("delete", configs.get("cleanup.policy"));
        assertEquals("1048576", configs.get("max.message.bytes"));
        assertTrue(KafkaAdminService.construirConfigs(null, null, null, null).isEmpty());
    }

    // ------------------------------------------------------------------ errores de Kafka -> errores de la API
    @Test
    void losErroresDeKafkaSeTraducenAExcepcionesDeLaApi() {
        assertInstanceOf(RecursoNoEncontradoException.class, KafkaAdminService.traducir(new UnknownTopicOrPartitionException("x")));
        assertInstanceOf(ConflictoException.class, KafkaAdminService.traducir(new TopicExistsException("x")));
        assertInstanceOf(ConflictoException.class, KafkaAdminService.traducir(new GroupNotEmptyException("x")));
        assertInstanceOf(IllegalArgumentException.class, KafkaAdminService.traducir(new InvalidReplicationFactorException("x")));
        assertInstanceOf(KafkaNoDisponibleException.class,
                KafkaAdminService.traducir(new org.apache.kafka.common.errors.TimeoutException("x")));
    }

    // ------------------------------------------------------------------ consumer groups
    @Test
    void describirUnGrupoCalculaElLagPorParticionYElTotal() {
        TopicPartition p0 = new TopicPartition("pedidos.eventos", 0);
        TopicPartition p1 = new TopicPartition("pedidos.eventos", 1);

        MemberDescription miembro = new MemberDescription("consumer-1", "ms-analitica-0", "/10.0.0.5",
                new MemberAssignment(Set.of(p0, p1)));
        ConsumerGroupDescription grupo = new ConsumerGroupDescription("analitica-ventas", false, List.of(miembro),
                "cooperative-sticky", ConsumerGroupState.STABLE, new Node(1, "kafka-1", 29092));
        DescribeConsumerGroupsResult describe = mock(DescribeConsumerGroupsResult.class);
        when(describe.all()).thenReturn(KafkaFuture.completedFuture(Map.of("analitica-ventas", grupo)));
        when(admin.describeConsumerGroups(anyCollection())).thenReturn(describe);

        ListConsumerGroupOffsetsResult offsets = mock(ListConsumerGroupOffsetsResult.class);
        when(offsets.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(
                Map.of(p0, new OffsetAndMetadata(10L), p1, new OffsetAndMetadata(7L))));
        when(admin.listConsumerGroupOffsets("analitica-ventas")).thenReturn(offsets);

        ListOffsetsResult finales = mock(ListOffsetsResult.class);
        when(finales.all()).thenReturn(KafkaFuture.completedFuture(Map.of(
                p0, new ListOffsetsResult.ListOffsetsResultInfo(15L, 0L, Optional.empty()),
                p1, new ListOffsetsResult.ListOffsetsResultInfo(7L, 0L, Optional.empty()))));
        when(admin.listOffsets(any())).thenReturn(finales);

        GroupDetail detalle = service.describirGrupo("analitica-ventas");

        assertEquals("STABLE", detalle.state());
        assertEquals("cooperative-sticky", detalle.partitionAssignor());
        assertEquals(1, detalle.members().size());
        assertEquals(List.of("pedidos.eventos-0", "pedidos.eventos-1"), detalle.members().get(0).partitions());
        assertEquals(2, detalle.offsets().size());
        assertEquals(5, detalle.offsets().get(0).lag());
        assertEquals(0, detalle.offsets().get(1).lag());
        assertEquals(5, detalle.totalLag());
    }

    @Test
    void unGrupoInexistenteEs404() {
        ConsumerGroupDescription muerto = new ConsumerGroupDescription("fantasma", false, List.of(), "",
                ConsumerGroupState.DEAD, new Node(1, "kafka-1", 29092));
        DescribeConsumerGroupsResult describe = mock(DescribeConsumerGroupsResult.class);
        when(describe.all()).thenReturn(KafkaFuture.completedFuture(Map.of("fantasma", muerto)));
        when(admin.describeConsumerGroups(anyCollection())).thenReturn(describe);
        ListConsumerGroupOffsetsResult offsets = mock(ListConsumerGroupOffsetsResult.class);
        when(offsets.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(Map.of()));
        when(admin.listConsumerGroupOffsets("fantasma")).thenReturn(offsets);

        assertThrows(RecursoNoEncontradoException.class, () -> service.describirGrupo("fantasma"));
    }

    // ------------------------------------------------------------------ mensajes de prueba
    @Test
    void noSePublicaEnUnTopicoQueNoExiste() {
        ListTopicsResult lista = mock(ListTopicsResult.class);
        when(lista.names()).thenReturn(KafkaFuture.completedFuture(Set.of("pedidos.eventos")));
        when(admin.listTopics(any(ListTopicsOptions.class))).thenReturn(lista);

        PublishRequest peticion = new PublishRequest();
        peticion.setTopic("otro.topico");
        peticion.setPayload("{}");

        assertThrows(RecursoNoEncontradoException.class, () -> service.publicar(peticion));
        verifyNoInteractions(kafkaTemplate);
    }
}
