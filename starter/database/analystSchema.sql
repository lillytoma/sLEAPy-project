

--Monetary trading vol
select sum(total_value :: numeric) as trade_volume
from sleapy_analytics.expensive_trades;


--Trading vol. in total
select COUNT(*) AS total_trades
FROM sleapy_analytics.expensive_trades;


--Most active instruments by descending
select 
	symbol,
	symbol_name,
	count(*) as total_trades
from sleapy_analytics.expensive_trades
group by
	symbol,
	symbol_name
order by
	total_trades desc;