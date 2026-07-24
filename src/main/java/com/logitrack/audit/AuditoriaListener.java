package com.logitrack.audit;

import com.logitrack.entities.Auditoria;
import com.logitrack.enums.TipoOperacion;
import com.logitrack.repositories.AuditoriaRepository;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Component
public class AuditoriaListener {

    private static AuditoriaRepository auditoriaRepository;

    @Autowired
    public void setAuditoriaRepository(AuditoriaRepository auditoriaRepository) {
        AuditoriaListener.auditoriaRepository = auditoriaRepository;
    }

    @PostPersist
    public void postPersist(Object entidad) {
        registrarAuditoria(entidad, TipoOperacion.INSERT);
    }

    @PostUpdate
    public void postUpdate(Object entidad) {
        registrarAuditoria(entidad, TipoOperacion.UPDATE);
    }

    @PostRemove
    public void postRemove(Object entidad) {
        registrarAuditoria(entidad, TipoOperacion.DELETE);
    }

    private void registrarAuditoria(Object entidad,
                                    TipoOperacion tipoOperacion) {

        if (auditoriaRepository == null) {
            return;
        }

        if (entidad instanceof Auditoria) {
            return;
        }

        Auditoria auditoria = new Auditoria();

        auditoria.setTipoOperacion(tipoOperacion);
        auditoria.setFechaHora(LocalDateTime.now());
        auditoria.setUsuario(obtenerUsuarioActual());
        auditoria.setEntidadAfectada(entidad.getClass().getSimpleName());
        auditoria.setEntidadId(obtenerId(entidad));
        auditoria.setValoresAnteriores(null);
        auditoria.setValoresNuevos(entidad.toString());

        auditoriaRepository.save(auditoria);
    }

    private Long obtenerId(Object entidad) {

        try {

            Method metodo = entidad.getClass().getMethod("getId");

            Object valor = metodo.invoke(entidad);

            if (valor instanceof Long) {
                return (Long) valor;
            }

        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private String obtenerUsuarioActual() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {

            return authentication.getName();
        }

        return "SISTEMA";
    }
}