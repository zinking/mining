
drop view USER_FEED;

--VIEW IS NOT ENOUGH
--NEED TO UPDATE USER_FEED information to JOIN the stat table
--TO Reflect the sub/unsub actions user made
CREATE TABLE IF NOT EXISTS USER_FEED (
  USER_ID BIGINT(10) NOT NULL,
  FEED_ID BIGINT(10) NOT NULL,
  PRIMARY KEY (USER_ID,FEED_ID)
) ;

insert into USER_FEED
select
user_id,
feed_id
from
  USER_STAT
where
  story_id = 0



