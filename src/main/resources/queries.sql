
SELECT
  TS.FEED_ID,
  TS.STORY_COUNT,
  TS.START_FROM,
  COALESCE(US.READ_COUNT,0) AS READ_COUNT
FROM
((
SELECT -- get user subscribed feeds and its story count
  F.FEED_ID,
  COUNT(*) AS STORY_COUNT,
  MAX(UU.START_FROM) AS START_FROM
FROM
  FEED_STORY F
  JOIN
  USER_STAT UU
  ON
    F.FEED_ID = UU.FEED_ID AND
    UU.STORY_ID = 0
WHERE
  F.PUBLISHED > UU.START_FROM AND
  UU.USER_ID = 1
GROUP BY
  F.FEED_ID
) TS
LEFT JOIN
(SELECT
  F.FEED_ID,
  COUNT(*) AS READ_COUNT
FROM
  FEED_STORY F
  JOIN
  USER_STAT U
  ON
    F.FEED_ID = U.FEED_ID AND
    F.STORY_ID = U.STORY_ID
  JOIN
  USER_STAT UU
  ON
    F.FEED_ID = UU.FEED_ID AND
    UU.STORY_ID = 0
WHERE
  F.PUBLISHED > UU.START_FROM AND
  U.USER_ID = 1
GROUP BY
	F.FEED_ID
) US
ON
  TS.FEED_ID = US.FEED_ID
)