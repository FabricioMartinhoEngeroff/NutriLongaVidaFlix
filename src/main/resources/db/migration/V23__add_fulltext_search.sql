
ALTER TABLE videos ADD COLUMN search_vector tsvector;
UPDATE videos
SET search_vector = to_tsvector('portuguese',
    coalesce(title, '') || ' ' || coalesce(description, ''));

CREATE INDEX videos_search_idx ON videos USING GIN(search_vector);

CREATE FUNCTION update_video_search() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        to_tsvector('portuguese',
            coalesce(NEW.title, '') || ' ' || coalesce(NEW.description, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER videos_search_update
    BEFORE INSERT OR UPDATE ON videos
    FOR EACH ROW EXECUTE FUNCTION update_video_search();
