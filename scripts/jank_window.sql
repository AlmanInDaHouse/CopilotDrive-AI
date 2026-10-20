-- Inclusive wall-time slices overlap; never sum categories as independent CPU time.
WITH bounds AS (
  SELECT ts, ts+dur AS end_ts FROM slice WHERE name='measureBlock' AND dur>0
), target_threads AS (
  SELECT t.utid, CASE WHEN t.is_main_thread THEN 'Main' ELSE 'RenderThread' END AS role
  FROM thread t JOIN process p USING(upid)
  WHERE p.name='ai.copilotdrive.app.benchmark' AND (t.is_main_thread OR t.name='RenderThread')
), clipped AS (
  SELECT role, s.name, min(s.ts+s.dur,b.end_ts)-max(s.ts,b.ts) AS overlap_ns
  FROM slice s JOIN thread_track tt ON s.track_id=tt.id
  JOIN target_threads t USING(utid), bounds b
  WHERE s.dur>0 AND s.ts<b.end_ts AND s.ts+s.dur>b.ts
)
SELECT 'window' AS category, 'measureBlock' AS label, count(*) AS n,
  sum(end_ts-ts)/1e6 AS value_ms FROM bounds
UNION ALL
SELECT 'scheduling', role||':'||s.state, count(*),
  sum(min(s.ts+s.dur,b.end_ts)-max(s.ts,b.ts))/1e6
FROM thread_state s JOIN target_threads t USING(utid), bounds b
WHERE s.dur>0 AND s.ts<b.end_ts AND s.ts+s.dur>b.ts GROUP BY role,s.state
UNION ALL
SELECT 'slice',role||':'||CASE
  WHEN name GLOB 'Choreographer#doFrame*' THEN 'Choreographer#doFrame'
  WHEN name GLOB 'DrawFrames*' THEN 'DrawFrames'
  ELSE name END, count(*),sum(overlap_ns)/1e6
FROM clipped WHERE name IN ('postAndWait','traversal','AndroidOwner:measureAndLayout',
  'Compose:recompose','compose:recompose','eglSwapBuffersWithDamageKHR','flush commands',
  'dequeueBuffer','compose:lazy:prefetch:compose','compose:lazy:prefetch:measure')
  OR name GLOB 'Choreographer#doFrame*' OR name GLOB 'DrawFrames*'
GROUP BY 2
UNION ALL
SELECT 'maxSlice',role||':'||name,count(*),max(overlap_ns)/1e6
FROM clipped WHERE name IN ('postAndWait','AndroidOwner:measureAndLayout',
  'Compose:recompose','binder transaction','eglSwapBuffersWithDamageKHR') GROUP BY role,name
UNION ALL
SELECT 'frameTimeline', a.jank_type, count(*), sum(min(a.ts+a.dur,b.end_ts)-max(a.ts,b.ts))/1e6
FROM actual_frame_timeline_slice a JOIN process p USING(upid), bounds b
WHERE p.name='ai.copilotdrive.app.benchmark' AND a.dur>0
  AND a.ts<b.end_ts AND a.ts+a.dur>b.ts GROUP BY a.jank_type
UNION ALL
SELECT 'traceErrors',name,sum(value),NULL FROM stats
WHERE severity='error' AND value>0 GROUP BY name;
