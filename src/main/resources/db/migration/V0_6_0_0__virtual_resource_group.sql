ALTER TABLE virtual_resource_group RENAME surf_conext_group_name TO surfconext_group_id;
ALTER TABLE virtual_resource_group DROP CONSTRAINT virtual_resource_group_name_key;
ALTER TABLE virtual_resource_group ADD COLUMN description character varying(255);