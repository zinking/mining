ALTER TABLE FEED_SOURCE ADD XML_URL VARCHAR(255);
ALTER TABLE FEED_SOURCE ADD TITLE VARCHAR(255);
ALTER TABLE FEED_SOURCE ADD TEXT VARCHAR(255);
ALTER TABLE FEED_SOURCE ADD HTML_URL VARCHAR(255);
ALTER TABLE FEED_SOURCE ADD FEED_TYPE VARCHAR(10);
ALTER TABLE FEED_SOURCE DROP COLUMN URL;
