package cl.duoc.pedidos360.ms_kafka_admin.controller;

import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.ClusterInfo;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.GroupDetail;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.GroupSummary;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.PublishResult;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.TopicDetail;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.TopicSummary;
import cl.duoc.pedidos360.ms_kafka_admin.dto.PartitionsRequest;
import cl.duoc.pedidos360.ms_kafka_admin.dto.PublishRequest;
import cl.duoc.pedidos360.ms_kafka_admin.dto.TopicConfigRequest;
import cl.duoc.pedidos360.ms_kafka_admin.dto.TopicRequest;
import cl.duoc.pedidos360.ms_kafka_admin.service.KafkaAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST de administracion de Kafka.
 *
 *   GET    /kafka/cluster                         brokers, controlador e id del cluster
 *   GET    /kafka/topics                          lista de topicos (?internos=true incluye los internos)
 *   POST   /kafka/topics                          crea un topico (particiones, replicas, retencion, cleanup)
 *   GET    /kafka/topics/{name}                   detalle: particiones, lider, replicas, ISR y configuracion
 *   PUT    /kafka/topics/{name}/config            cambia retencion / cleanup.policy / min.insync.replicas
 *   PUT    /kafka/topics/{name}/partitions        aumenta el numero de particiones
 *   DELETE /kafka/topics/{name}                   elimina el topico
 *   GET    /kafka/consumer-groups                 lista los consumer groups
 *   GET    /kafka/consumer-groups/{groupId}       miembros, offsets y lag
 *   DELETE /kafka/consumer-groups/{groupId}       elimina el grupo (debe estar sin miembros)
 *   POST   /kafka/messages                        publica un mensaje de prueba
 */
@RestController
@RequestMapping("/kafka")
@RequiredArgsConstructor
public class KafkaAdminController {

    private final KafkaAdminService service;

    // ------------------------------------------------------------------ cluster
    @GetMapping("/cluster")
    public ClusterInfo cluster() {
        return service.cluster();
    }

    // ------------------------------------------------------------------ topicos
    @GetMapping("/topics")
    public List<TopicSummary> listarTopicos(@RequestParam(defaultValue = "false") boolean internos) {
        return service.listarTopicos(internos);
    }

    @PostMapping("/topics")
    public ResponseEntity<TopicDetail> crearTopico(@Valid @RequestBody TopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearTopico(request));
    }

    @GetMapping("/topics/{name}")
    public TopicDetail describirTopico(@PathVariable String name) {
        return service.describirTopico(name);
    }

    @PutMapping("/topics/{name}/config")
    public TopicDetail actualizarConfiguracion(@PathVariable String name,
                                               @Valid @RequestBody TopicConfigRequest request) {
        return service.actualizarConfiguracion(name, request);
    }

    @PutMapping("/topics/{name}/partitions")
    public TopicDetail aumentarParticiones(@PathVariable String name,
                                           @Valid @RequestBody PartitionsRequest request) {
        return service.aumentarParticiones(name, request.getPartitions());
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Void> eliminarTopico(@PathVariable String name) {
        service.eliminarTopico(name);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ consumer groups
    @GetMapping("/consumer-groups")
    public List<GroupSummary> listarGrupos() {
        return service.listarGrupos();
    }

    @GetMapping("/consumer-groups/{groupId}")
    public GroupDetail describirGrupo(@PathVariable String groupId) {
        return service.describirGrupo(groupId);
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    public ResponseEntity<Void> eliminarGrupo(@PathVariable String groupId) {
        service.eliminarGrupo(groupId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ mensajes de prueba
    @PostMapping("/messages")
    public ResponseEntity<PublishResult> publicar(@Valid @RequestBody PublishRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.publicar(request));
    }
}
