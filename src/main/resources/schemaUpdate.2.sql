CREATE TABLE IF NOT EXISTS FEED_STRUCTURE (
  FEED_ID BIGINT(10) NOT NULL,
  START_URL VARCHAR(255) NOT NULL,
  STORY  VARCHAR (1255),
  TITLE  VARCHAR (1255),
  LINK   VARCHAR (1255),
  SUMMARY VARCHAR (1255),
  CONTENT VARCHAR (1255),
  AUTHOR VARCHAR (1255),
  CREATED VARCHAR (1255),
  UPDATED VARCHAR (1255),
  PRIMARY KEY (FEED_ID)
) ;

-- new version
ALTER TABLE FEED_STRUCTURE ADD START VARCHAR(1255);