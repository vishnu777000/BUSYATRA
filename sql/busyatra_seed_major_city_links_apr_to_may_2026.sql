-- BusYatra metro-city route expansion
-- Adds major AP to metro-city corridors with multiple daily timings.

SET @metro_seed_start = GREATEST(CURDATE(), DATE('2026-04-06'));
SET @metro_seed_end = DATE('2026-05-31');

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS seed_cities_metro;
CREATE TEMPORARY TABLE seed_cities_metro (
    name VARCHAR(100) PRIMARY KEY
);

INSERT INTO seed_cities_metro (name) VALUES
('Bangalore'),
('Chennai'),
('Mumbai'),
('Pune'),
('Bhubaneswar'),
('Kolkata'),
('Solapur'),
('Kharagpur'),
('Vellore'),
('Chittoor');

INSERT INTO cities (name)
SELECT s.name
FROM seed_cities_metro s
WHERE NOT EXISTS (
    SELECT 1 FROM cities c WHERE UPPER(TRIM(c.name)) = UPPER(TRIM(s.name))
);

DROP TEMPORARY TABLE IF EXISTS seed_buses_metro;
CREATE TEMPORARY TABLE seed_buses_metro (
    operator_name VARCHAR(120) PRIMARY KEY,
    bus_type_value VARCHAR(20) NOT NULL,
    seat_count INT NOT NULL,
    fare_multiplier DECIMAL(5,2) NOT NULL
);

INSERT INTO seed_buses_metro (operator_name, bus_type_value, seat_count, fare_multiplier) VALUES
('CityLink Day 01', 'NON_AC', 46, 0.98),
('CityLink Day 02', 'NON_AC', 46, 0.98),
('CityLink Day 03', 'NON_AC', 46, 0.98),
('CityLink Day 04', 'NON_AC', 46, 0.98),
('CityLink Day 05', 'NON_AC', 46, 0.98),
('CityLink Day 06', 'NON_AC', 46, 0.98),
('Metro Comfort 01', 'AC', 40, 1.12),
('Metro Comfort 02', 'AC', 40, 1.12),
('Metro Comfort 03', 'AC', 40, 1.12),
('Metro Comfort 04', 'AC', 40, 1.12),
('Metro Comfort 05', 'AC', 40, 1.12),
('Metro Comfort 06', 'AC', 40, 1.12),
('Metro Comfort 07', 'AC', 40, 1.12),
('Metro Comfort 08', 'AC', 40, 1.12),
('Metro Comfort 09', 'AC', 40, 1.12),
('Metro Comfort 10', 'AC', 40, 1.12),
('Metro Comfort 11', 'AC', 40, 1.12),
('Metro Comfort 12', 'AC', 40, 1.12),
('Metro Comfort 13', 'AC', 40, 1.12),
('Metro Comfort 14', 'AC', 40, 1.12),
('Metro Comfort 15', 'AC', 40, 1.12),
('Metro Comfort 16', 'AC', 40, 1.12),
('Metro Comfort 17', 'AC', 40, 1.12),
('Metro Comfort 18', 'AC', 40, 1.12),
('Night Arrow 01', 'SLEEPER', 30, 1.30),
('Night Arrow 02', 'SLEEPER', 30, 1.30),
('Night Arrow 03', 'SLEEPER', 30, 1.30),
('Night Arrow 04', 'SLEEPER', 30, 1.30),
('Night Arrow 05', 'SLEEPER', 30, 1.30),
('Night Arrow 06', 'SLEEPER', 30, 1.30),
('Night Arrow 07', 'SLEEPER', 30, 1.30),
('Night Arrow 08', 'SLEEPER', 30, 1.30),
('Night Arrow 09', 'SLEEPER', 30, 1.30),
('Night Arrow 10', 'SLEEPER', 30, 1.30),
('Night Arrow 11', 'SLEEPER', 30, 1.30),
('Night Arrow 12', 'SLEEPER', 30, 1.30),
('Night Arrow 13', 'SLEEPER', 30, 1.30),
('Night Arrow 14', 'SLEEPER', 30, 1.30),
('Night Arrow 15', 'SLEEPER', 30, 1.30),
('Night Arrow 16', 'SLEEPER', 30, 1.30);

INSERT INTO buses (operator, bus_type, total_seats, fare_multiplier)
SELECT s.operator_name, s.bus_type_value, s.seat_count, s.fare_multiplier
FROM seed_buses_metro s
WHERE NOT EXISTS (
    SELECT 1
    FROM buses b
    WHERE UPPER(TRIM(b.operator)) = UPPER(TRIM(s.operator_name))
      AND UPPER(TRIM(b.bus_type)) = UPPER(TRIM(s.bus_type_value))
);

DROP TEMPORARY TABLE IF EXISTS seed_routes_metro;
CREATE TEMPORARY TABLE seed_routes_metro (
    source_city VARCHAR(100) NOT NULL,
    destination_city VARCHAR(100) NOT NULL,
    base_fare DECIMAL(10,2) NOT NULL,
    route_map VARCHAR(1024) NULL,
    PRIMARY KEY (source_city, destination_city)
);

INSERT INTO seed_routes_metro (source_city, destination_city, base_fare, route_map) VALUES
('Vijayawada', 'Bangalore', 3.15, NULL),
('Bangalore', 'Vijayawada', 3.15, NULL),
('Tirupati', 'Bangalore', 3.00, NULL),
('Bangalore', 'Tirupati', 3.00, NULL),
('Tirupati', 'Chennai', 2.90, NULL),
('Chennai', 'Tirupati', 2.90, NULL),
('Vijayawada', 'Chennai', 3.05, NULL),
('Chennai', 'Vijayawada', 3.05, NULL),
('Hyderabad', 'Bangalore', 3.20, NULL),
('Bangalore', 'Hyderabad', 3.20, NULL),
('Hyderabad', 'Pune', 3.35, NULL),
('Pune', 'Hyderabad', 3.35, NULL),
('Hyderabad', 'Mumbai', 3.45, NULL),
('Mumbai', 'Hyderabad', 3.45, NULL),
('Visakhapatnam', 'Bhubaneswar', 3.00, NULL),
('Bhubaneswar', 'Visakhapatnam', 3.00, NULL),
('Visakhapatnam', 'Kolkata', 3.10, NULL),
('Kolkata', 'Visakhapatnam', 3.10, NULL),
('Vijayawada', 'Bhubaneswar', 3.08, NULL),
('Bhubaneswar', 'Vijayawada', 3.08, NULL);

INSERT INTO routes (source_city_id, destination_city_id, base_fare, route_map)
SELECT src.id, dst.id, s.base_fare, s.route_map
FROM seed_routes_metro s
JOIN cities src ON UPPER(TRIM(src.name)) = UPPER(TRIM(s.source_city))
JOIN cities dst ON UPPER(TRIM(dst.name)) = UPPER(TRIM(s.destination_city))
WHERE NOT EXISTS (
    SELECT 1
    FROM routes r
    WHERE r.source_city_id = src.id
      AND r.destination_city_id = dst.id
);

DROP TEMPORARY TABLE IF EXISTS seed_route_stops_metro;
CREATE TEMPORARY TABLE seed_route_stops_metro (
    source_city VARCHAR(100) NOT NULL,
    destination_city VARCHAR(100) NOT NULL,
    stop_city VARCHAR(100) NOT NULL,
    stop_order_value INT NOT NULL,
    distance_value DECIMAL(10,2) NOT NULL,
    arrival_offset_value INT NULL,
    departure_offset_value INT NULL,
    PRIMARY KEY (source_city, destination_city, stop_order_value)
);

INSERT INTO seed_route_stops_metro (
    source_city, destination_city, stop_city, stop_order_value, distance_value, arrival_offset_value, departure_offset_value
) VALUES
('Vijayawada', 'Bangalore', 'Vijayawada', 1, 0, 0, 0),
('Vijayawada', 'Bangalore', 'Guntur', 2, 35, 45, 50),
('Vijayawada', 'Bangalore', 'Ongole', 3, 145, 150, 155),
('Vijayawada', 'Bangalore', 'Nellore', 4, 280, 290, 295),
('Vijayawada', 'Bangalore', 'Chittoor', 5, 520, 560, 565),
('Vijayawada', 'Bangalore', 'Bangalore', 6, 710, 795, 795),
('Bangalore', 'Vijayawada', 'Bangalore', 1, 0, 0, 0),
('Bangalore', 'Vijayawada', 'Chittoor', 2, 190, 215, 220),
('Bangalore', 'Vijayawada', 'Nellore', 3, 430, 500, 505),
('Bangalore', 'Vijayawada', 'Ongole', 4, 565, 645, 650),
('Bangalore', 'Vijayawada', 'Guntur', 5, 675, 750, 755),
('Bangalore', 'Vijayawada', 'Vijayawada', 6, 710, 795, 795),
('Tirupati', 'Bangalore', 'Tirupati', 1, 0, 0, 0),
('Tirupati', 'Bangalore', 'Chittoor', 2, 65, 70, 75),
('Tirupati', 'Bangalore', 'Bangalore', 3, 250, 300, 300),
('Bangalore', 'Tirupati', 'Bangalore', 1, 0, 0, 0),
('Bangalore', 'Tirupati', 'Chittoor', 2, 185, 225, 230),
('Bangalore', 'Tirupati', 'Tirupati', 3, 250, 300, 300),
('Tirupati', 'Chennai', 'Tirupati', 1, 0, 0, 0),
('Tirupati', 'Chennai', 'Naidupeta', 2, 80, 90, 95),
('Tirupati', 'Chennai', 'Vellore', 3, 145, 155, 160),
('Tirupati', 'Chennai', 'Chennai', 4, 195, 240, 240),
('Chennai', 'Tirupati', 'Chennai', 1, 0, 0, 0),
('Chennai', 'Tirupati', 'Vellore', 2, 50, 60, 65),
('Chennai', 'Tirupati', 'Naidupeta', 3, 115, 145, 150),
('Chennai', 'Tirupati', 'Tirupati', 4, 195, 240, 240),
('Vijayawada', 'Chennai', 'Vijayawada', 1, 0, 0, 0),
('Vijayawada', 'Chennai', 'Ongole', 2, 145, 150, 155),
('Vijayawada', 'Chennai', 'Nellore', 3, 280, 290, 295),
('Vijayawada', 'Chennai', 'Naidupeta', 4, 370, 385, 390),
('Vijayawada', 'Chennai', 'Chennai', 5, 450, 555, 555),
('Chennai', 'Vijayawada', 'Chennai', 1, 0, 0, 0),
('Chennai', 'Vijayawada', 'Naidupeta', 2, 80, 95, 100),
('Chennai', 'Vijayawada', 'Nellore', 3, 170, 195, 200),
('Chennai', 'Vijayawada', 'Ongole', 4, 305, 340, 345),
('Chennai', 'Vijayawada', 'Vijayawada', 5, 450, 555, 555),
('Hyderabad', 'Bangalore', 'Hyderabad', 1, 0, 0, 0),
('Hyderabad', 'Bangalore', 'Kurnool', 2, 220, 240, 245),
('Hyderabad', 'Bangalore', 'Anantapur', 3, 375, 420, 425),
('Hyderabad', 'Bangalore', 'Bangalore', 4, 570, 660, 660),
('Bangalore', 'Hyderabad', 'Bangalore', 1, 0, 0, 0),
('Bangalore', 'Hyderabad', 'Anantapur', 2, 195, 240, 245),
('Bangalore', 'Hyderabad', 'Kurnool', 3, 350, 420, 425),
('Bangalore', 'Hyderabad', 'Hyderabad', 4, 570, 660, 660),
('Hyderabad', 'Pune', 'Hyderabad', 1, 0, 0, 0),
('Hyderabad', 'Pune', 'Solapur', 2, 300, 330, 335),
('Hyderabad', 'Pune', 'Pune', 3, 560, 600, 600),
('Pune', 'Hyderabad', 'Pune', 1, 0, 0, 0),
('Pune', 'Hyderabad', 'Solapur', 2, 260, 285, 290),
('Pune', 'Hyderabad', 'Hyderabad', 3, 560, 600, 600),
('Hyderabad', 'Mumbai', 'Hyderabad', 1, 0, 0, 0),
('Hyderabad', 'Mumbai', 'Solapur', 2, 300, 330, 335),
('Hyderabad', 'Mumbai', 'Pune', 3, 560, 610, 615),
('Hyderabad', 'Mumbai', 'Mumbai', 4, 710, 780, 780),
('Mumbai', 'Hyderabad', 'Mumbai', 1, 0, 0, 0),
('Mumbai', 'Hyderabad', 'Pune', 2, 150, 180, 185),
('Mumbai', 'Hyderabad', 'Solapur', 3, 410, 470, 475),
('Mumbai', 'Hyderabad', 'Hyderabad', 4, 710, 780, 780),
('Visakhapatnam', 'Bhubaneswar', 'Visakhapatnam', 1, 0, 0, 0),
('Visakhapatnam', 'Bhubaneswar', 'Srikakulam', 2, 125, 140, 145),
('Visakhapatnam', 'Bhubaneswar', 'Bhubaneswar', 3, 450, 510, 510),
('Bhubaneswar', 'Visakhapatnam', 'Bhubaneswar', 1, 0, 0, 0),
('Bhubaneswar', 'Visakhapatnam', 'Srikakulam', 2, 325, 370, 375),
('Bhubaneswar', 'Visakhapatnam', 'Visakhapatnam', 3, 450, 510, 510),
('Visakhapatnam', 'Kolkata', 'Visakhapatnam', 1, 0, 0, 0),
('Visakhapatnam', 'Kolkata', 'Srikakulam', 2, 125, 140, 145),
('Visakhapatnam', 'Kolkata', 'Bhubaneswar', 3, 450, 520, 525),
('Visakhapatnam', 'Kolkata', 'Kharagpur', 4, 750, 840, 845),
('Visakhapatnam', 'Kolkata', 'Kolkata', 5, 870, 990, 990),
('Kolkata', 'Visakhapatnam', 'Kolkata', 1, 0, 0, 0),
('Kolkata', 'Visakhapatnam', 'Kharagpur', 2, 120, 150, 155),
('Kolkata', 'Visakhapatnam', 'Bhubaneswar', 3, 420, 470, 475),
('Kolkata', 'Visakhapatnam', 'Srikakulam', 4, 745, 850, 855),
('Kolkata', 'Visakhapatnam', 'Visakhapatnam', 5, 870, 990, 990),
('Vijayawada', 'Bhubaneswar', 'Vijayawada', 1, 0, 0, 0),
('Vijayawada', 'Bhubaneswar', 'Rajahmundry', 2, 160, 170, 175),
('Vijayawada', 'Bhubaneswar', 'Visakhapatnam', 3, 350, 400, 405),
('Vijayawada', 'Bhubaneswar', 'Srikakulam', 4, 475, 555, 560),
('Vijayawada', 'Bhubaneswar', 'Bhubaneswar', 5, 800, 960, 960),
('Bhubaneswar', 'Vijayawada', 'Bhubaneswar', 1, 0, 0, 0),
('Bhubaneswar', 'Vijayawada', 'Srikakulam', 2, 325, 405, 410),
('Bhubaneswar', 'Vijayawada', 'Visakhapatnam', 3, 450, 560, 565),
('Bhubaneswar', 'Vijayawada', 'Rajahmundry', 4, 640, 790, 795),
('Bhubaneswar', 'Vijayawada', 'Vijayawada', 5, 800, 960, 960);

INSERT INTO route_stops (route_id, city_id, stop_order, distance_from_start, arrival_offset, departure_offset)
SELECT r.id, c.id, s.stop_order_value, s.distance_value, s.arrival_offset_value, s.departure_offset_value
FROM seed_route_stops_metro s
JOIN cities src ON UPPER(TRIM(src.name)) = UPPER(TRIM(s.source_city))
JOIN cities dst ON UPPER(TRIM(dst.name)) = UPPER(TRIM(s.destination_city))
JOIN routes r ON r.source_city_id = src.id AND r.destination_city_id = dst.id
JOIN cities c ON UPPER(TRIM(c.name)) = UPPER(TRIM(s.stop_city))
WHERE NOT EXISTS (
    SELECT 1
    FROM route_stops rs
    WHERE rs.route_id = r.id
      AND rs.stop_order = s.stop_order_value
);

DROP TEMPORARY TABLE IF EXISTS seed_schedule_templates_metro;
CREATE TEMPORARY TABLE seed_schedule_templates_metro (
    source_city VARCHAR(100) NOT NULL,
    destination_city VARCHAR(100) NOT NULL,
    operator_name VARCHAR(120) NOT NULL,
    bus_type_value VARCHAR(20) NOT NULL,
    departure_time_value TIME NOT NULL,
    arrival_time_value TIME NOT NULL,
    PRIMARY KEY (source_city, destination_city, operator_name, departure_time_value)
);

INSERT INTO seed_schedule_templates_metro (
    source_city, destination_city, operator_name, bus_type_value, departure_time_value, arrival_time_value
) VALUES
('Vijayawada', 'Bangalore', 'Metro Comfort 01', 'AC', '06:00:00', '19:15:00'),
('Vijayawada', 'Bangalore', 'Night Arrow 01', 'SLEEPER', '21:00:00', '08:15:00'),
('Bangalore', 'Vijayawada', 'Metro Comfort 02', 'AC', '06:00:00', '19:15:00'),
('Bangalore', 'Vijayawada', 'Night Arrow 02', 'SLEEPER', '21:00:00', '08:15:00'),
('Tirupati', 'Bangalore', 'CityLink Day 01', 'NON_AC', '07:00:00', '12:00:00'),
('Tirupati', 'Bangalore', 'Metro Comfort 03', 'AC', '17:30:00', '22:15:00'),
('Bangalore', 'Tirupati', 'CityLink Day 02', 'NON_AC', '07:00:00', '12:00:00'),
('Bangalore', 'Tirupati', 'Metro Comfort 04', 'AC', '17:30:00', '22:15:00'),
('Tirupati', 'Chennai', 'CityLink Day 03', 'NON_AC', '06:30:00', '10:30:00'),
('Tirupati', 'Chennai', 'Metro Comfort 05', 'AC', '16:30:00', '20:30:00'),
('Chennai', 'Tirupati', 'CityLink Day 04', 'NON_AC', '06:30:00', '10:30:00'),
('Chennai', 'Tirupati', 'Metro Comfort 06', 'AC', '16:30:00', '20:30:00'),
('Vijayawada', 'Chennai', 'Metro Comfort 07', 'AC', '07:15:00', '16:30:00'),
('Vijayawada', 'Chennai', 'Night Arrow 03', 'SLEEPER', '21:30:00', '06:00:00'),
('Chennai', 'Vijayawada', 'Metro Comfort 08', 'AC', '07:15:00', '16:30:00'),
('Chennai', 'Vijayawada', 'Night Arrow 04', 'SLEEPER', '21:30:00', '06:00:00'),
('Hyderabad', 'Bangalore', 'Metro Comfort 09', 'AC', '06:30:00', '16:30:00'),
('Hyderabad', 'Bangalore', 'Night Arrow 05', 'SLEEPER', '21:00:00', '07:00:00'),
('Bangalore', 'Hyderabad', 'Metro Comfort 10', 'AC', '06:30:00', '16:30:00'),
('Bangalore', 'Hyderabad', 'Night Arrow 06', 'SLEEPER', '21:00:00', '07:00:00'),
('Hyderabad', 'Pune', 'Metro Comfort 11', 'AC', '07:00:00', '17:00:00'),
('Hyderabad', 'Pune', 'Night Arrow 07', 'SLEEPER', '21:15:00', '06:45:00'),
('Pune', 'Hyderabad', 'Metro Comfort 12', 'AC', '07:00:00', '17:00:00'),
('Pune', 'Hyderabad', 'Night Arrow 08', 'SLEEPER', '21:15:00', '06:45:00'),
('Hyderabad', 'Mumbai', 'Metro Comfort 13', 'AC', '06:00:00', '19:00:00'),
('Hyderabad', 'Mumbai', 'Night Arrow 09', 'SLEEPER', '20:30:00', '07:15:00'),
('Mumbai', 'Hyderabad', 'Metro Comfort 14', 'AC', '06:00:00', '19:00:00'),
('Mumbai', 'Hyderabad', 'Night Arrow 10', 'SLEEPER', '20:30:00', '07:15:00'),
('Visakhapatnam', 'Bhubaneswar', 'CityLink Day 05', 'NON_AC', '06:00:00', '14:30:00'),
('Visakhapatnam', 'Bhubaneswar', 'Night Arrow 11', 'SLEEPER', '21:30:00', '05:45:00'),
('Bhubaneswar', 'Visakhapatnam', 'CityLink Day 06', 'NON_AC', '06:00:00', '14:30:00'),
('Bhubaneswar', 'Visakhapatnam', 'Night Arrow 12', 'SLEEPER', '21:30:00', '05:45:00'),
('Visakhapatnam', 'Kolkata', 'Metro Comfort 15', 'AC', '05:45:00', '22:15:00'),
('Visakhapatnam', 'Kolkata', 'Night Arrow 13', 'SLEEPER', '18:00:00', '08:45:00'),
('Kolkata', 'Visakhapatnam', 'Metro Comfort 16', 'AC', '05:45:00', '22:15:00'),
('Kolkata', 'Visakhapatnam', 'Night Arrow 14', 'SLEEPER', '18:00:00', '08:45:00'),
('Vijayawada', 'Bhubaneswar', 'Metro Comfort 17', 'AC', '05:15:00', '20:15:00'),
('Vijayawada', 'Bhubaneswar', 'Night Arrow 15', 'SLEEPER', '18:45:00', '08:00:00'),
('Bhubaneswar', 'Vijayawada', 'Metro Comfort 18', 'AC', '05:15:00', '20:15:00'),
('Bhubaneswar', 'Vijayawada', 'Night Arrow 16', 'SLEEPER', '18:45:00', '08:00:00');

DROP TEMPORARY TABLE IF EXISTS seed_dates_metro;
CREATE TEMPORARY TABLE seed_dates_metro (
    service_date DATE PRIMARY KEY
);

INSERT INTO seed_dates_metro (service_date)
SELECT DATE_ADD(@metro_seed_start, INTERVAL nums.n DAY)
FROM (
    SELECT ones.n + tens.n * 10 AS n
    FROM (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) ones
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) tens
) nums
WHERE @metro_seed_start <= @metro_seed_end
  AND nums.n <= DATEDIFF(@metro_seed_end, @metro_seed_start);

INSERT INTO schedules (route_id, bus_id, departure_time, arrival_time, status)
SELECT r.id,
       b.id,
       TIMESTAMP(d.service_date, t.departure_time_value),
       CASE
           WHEN t.arrival_time_value > t.departure_time_value THEN TIMESTAMP(d.service_date, t.arrival_time_value)
           ELSE TIMESTAMP(DATE_ADD(d.service_date, INTERVAL 1 DAY), t.arrival_time_value)
       END,
       'ACTIVE'
FROM seed_dates_metro d
JOIN seed_schedule_templates_metro t
JOIN cities src ON UPPER(TRIM(src.name)) = UPPER(TRIM(t.source_city))
JOIN cities dst ON UPPER(TRIM(dst.name)) = UPPER(TRIM(t.destination_city))
JOIN routes r ON r.source_city_id = src.id AND r.destination_city_id = dst.id
JOIN buses b ON UPPER(TRIM(b.operator)) = UPPER(TRIM(t.operator_name))
            AND UPPER(TRIM(b.bus_type)) = UPPER(TRIM(t.bus_type_value))
WHERE NOT EXISTS (
    SELECT 1
    FROM schedules s
    WHERE s.route_id = r.id
      AND s.bus_id = b.id
      AND s.departure_time = TIMESTAMP(d.service_date, t.departure_time_value)
);

COMMIT;

SELECT COUNT(*) AS metro_route_pairs
FROM routes r
JOIN cities src ON src.id = r.source_city_id
JOIN cities dst ON dst.id = r.destination_city_id
WHERE src.name IN ('Vijayawada', 'Tirupati', 'Hyderabad', 'Visakhapatnam', 'Bhubaneswar', 'Bangalore', 'Chennai', 'Mumbai', 'Pune', 'Kolkata')
  AND dst.name IN ('Vijayawada', 'Tirupati', 'Hyderabad', 'Visakhapatnam', 'Bhubaneswar', 'Bangalore', 'Chennai', 'Mumbai', 'Pune', 'Kolkata');
