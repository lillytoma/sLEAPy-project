package com.sleapy.project.mappers;

import com.sleapy.project.models.entities.AuditLogEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis mapper for Audit_Log table operations
 */
@Mapper
public interface AuditLogMapper {
    
    /**
     * Insert a new audit log entry
     */
    @Insert("INSERT INTO Audit_Log (Client_ID, Activity_Type, Timestamp, IP_Address, Details, Source_System) " +
            "VALUES (#{clientId}, #{activityType}, #{timestamp}, #{ipAddress}, #{details}, #{sourceSystem})")
    int insertAuditLog(AuditLogEntity auditLog);
    
    /**
     * Get active users in the last 24 hours (distinct clients with login events)
     */
    @Select("SELECT DISTINCT Client_ID FROM Audit_Log " +
            "WHERE Activity_Type IN ('ClientLoginAttempted', 'ClientLoginSucceeded') " +
            "AND Timestamp >= NOW() - INTERVAL '24 hours' " +
            "ORDER BY Client_ID")
    List<Long> getActiveUsersLast24Hours();
    
    /**
     * Count active users in the last 24 hours
     */
    @Select("SELECT COUNT(DISTINCT Client_ID) FROM Audit_Log " +
            "WHERE Activity_Type IN ('ClientLoginAttempted', 'ClientLoginSucceeded') " +
            "AND Timestamp >= NOW() - INTERVAL '24 hours'")
    int countActiveUsersLast24Hours();
    
    /**
     * Get audit logs for a specific client
     */
    @Select("SELECT * FROM Audit_Log WHERE Client_ID = #{clientId} ORDER BY Timestamp DESC LIMIT #{limit}")
    List<AuditLogEntity> getClientAuditLogs(@Param("clientId") Long clientId, @Param("limit") int limit);
    
    /**
     * Get recent audit logs by activity type
     */
    @Select("SELECT * FROM Audit_Log WHERE Activity_Type = #{activityType} ORDER BY Timestamp DESC LIMIT #{limit}")
    List<AuditLogEntity> getAuditLogsByType(@Param("activityType") String activityType, @Param("limit") int limit);
}
