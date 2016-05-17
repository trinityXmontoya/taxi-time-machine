-- name: query-trip-by-datetime-range
SELECT *
FROM trips
WHERE tsrange(pickup_datetime, dropoff_datetime, '[]')
      @>
      :datetime::timestamp
AND ST_Intersects(
  ST_MakeLine(ST_Point(dropoff_longitude, dropoff_latitude),
              ST_Point(pickup_longitude, pickup_latitude)),
  ST_GeomFromText(:bbox_polygon))
LIMIT 1;
