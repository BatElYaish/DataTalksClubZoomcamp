select *
from {{ ref('fact_trips') }}
where CAST(pickup_datetime AS DATETIME) >= CURRENT_DATE() - INTERVAL  '{{ var("days_back", env_var("DBT_DAYS_BACK", "30")) }}' DAY