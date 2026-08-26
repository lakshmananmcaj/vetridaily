-- VetriDaily — content bridge migration.
-- Run once in the Supabase SQL editor before scripts/import_content_plan.py.
--
-- Links a daily_content row back to the SocialMediaTool ContentPlanItem it came
-- from, so the importer can run nightly without ever duplicating a day.

alter table daily_content
    add column if not exists source_item_id integer;

-- One row per source item. Rows authored by hand keep source_item_id NULL,
-- and NULLs are not compared by a unique index, so any number of them is fine.
create unique index if not exists daily_content_source_item_id_key
    on daily_content (source_item_id)
    where source_item_id is not null;
