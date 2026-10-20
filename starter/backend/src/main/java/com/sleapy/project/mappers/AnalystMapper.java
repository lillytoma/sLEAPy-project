package com.sleapy.project.mappers;

import com.sleapy.project.models.dtos.AnalystInstrumentActivityDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnalystMapper {
    @Select("""
            SELECT client_id AS id, username, email
            FROM clients
            WHERE LOWER(username) LIKE LOWER(CONCAT('%', #{search}, '%'))
               OR LOWER(email) LIKE LOWER(CONCAT('%', #{search}, '%'))
            ORDER BY username ASC
            LIMIT 20
            """)
    List<com.sleapy.project.models.dtos.AnalystClientDTO> findClients(@Param("search") String search);

    @Select("""
            SELECT COALESCE(SUM(total_value::numeric), 0)
            FROM sleapy_analytics.expensive_trades
            """)
    BigDecimal findTradeVolume();

    @Select("""
            SELECT COUNT(*)
            FROM sleapy_analytics.expensive_trades
            """)
    long findTotalTrades();

    @Select("""
            SELECT COALESCE(SUM(o.quantity * o.purchase_price), 0)
            FROM orders o
            WHERE o.client_id = #{clientId}
            """)
    BigDecimal findTradeVolumeByClient(@Param("clientId") Long clientId);

    @Select("""
            SELECT COUNT(*)
            FROM orders
            WHERE client_id = #{clientId}
            """)
    long findTotalTradesByClient(@Param("clientId") Long clientId);

    @Select("""
            SELECT
                et.symbol,
                et.symbol_name,
                COALESCE(it.instrumenttype, 'Other') AS instrument_type,
                COUNT(*) AS total_trades,
                COALESCE(SUM(et.total_value::numeric), 0) AS trade_volume
            FROM sleapy_analytics.expensive_trades
            et
            LEFT JOIN instruments i ON i.symbol = et.symbol
            LEFT JOIN instrument_type it ON it.instrumenttype_id = i.instrumenttype_id
            GROUP BY et.symbol, et.symbol_name, it.instrumenttype
            ORDER BY total_trades DESC, symbol ASC
            """)
    List<AnalystInstrumentActivityDTO> findMostActiveInstruments();

    @Select("""
            SELECT
                i.symbol,
                i.symbol_name,
                COALESCE(it.instrumenttype, 'Other') AS instrument_type,
                COUNT(*) AS total_trades,
                COALESCE(SUM(o.quantity * o.purchase_price), 0) AS trade_volume
            FROM orders o
            JOIN instruments i ON i.instrument_id = o.instrument_id
            LEFT JOIN instrument_type it ON it.instrumenttype_id = i.instrumenttype_id
            WHERE o.client_id = #{clientId}
            GROUP BY i.symbol, i.symbol_name, it.instrumenttype
            ORDER BY total_trades DESC, i.symbol ASC
            """)
    List<AnalystInstrumentActivityDTO> findMostActiveInstrumentsByClient(@Param("clientId") Long clientId);
}
