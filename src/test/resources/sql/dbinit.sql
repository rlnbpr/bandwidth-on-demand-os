insert into physical_resource_group (id, admin_group, institution_name, name, version) values (1, 'urn:collab:group:test.surfteams.nl:nl:surfnet:diensten:bandwidth-on-demand', 'Mock_Klantnaam', 'prg1', 0);
insert into physical_resource_group (id, admin_group, institution_name, name, version) values (2, 'urn:collab:group:test.surfteams.nl:nl:surfnet:diensten:ict-managers', 'Technische Universiteit Delft', 'prg Delft', 0);

insert into physical_port (id, display_name, name, physical_resource_group, version) values (1, 'remote.5410-02t.asd001a.dcn.surf.net_Port5/1', '00:03:18:bc:76:00_Port5/1_dummy', 1, 0);
insert into physical_port (id, display_name, name, physical_resource_group, version) values (2, 'remote.5410-02t.asd001a.dcn.surf.net_Port1/8', '00:03:18:bc:76:00_Port1/8_dummy', 1, 0);
insert into physical_port (id, display_name, name, physical_resource_group, version) values (3, 'remote.5410-02t.asd001a.dcn.surf.net_Portto-5410_03T', '00:03:18:bc:76:00_Port2050_dummy', 1, 0);
insert into physical_port (id, display_name, name, physical_resource_group, version) values (4, 'remote.5410-02t.asd001a.dcn.surf.net_Port5/4', '00:03:18:bc:76:00_Port5/4_dummy', 1, 0);

insert into physical_port (id, display_name, name, physical_resource_group, version) values (5, 'mgmt.3960-02t.asd001a.dcn.surf.net_Port1', '00:03:18:80:7c:e0_Port1_dummy', 2, 0);
insert into physical_port (id, display_name, name, physical_resource_group, version) values (6, '3960-01.asd001a.dcn.surf.net_Port2049', '00:03:18:87:06:40_Port2049_dummy', 2, 0);

insert into virtual_resource_group (id, name, surf_connext_group_name, version) values (1, 'vrg1', 'urn:collab:group:test.surfteams.nl:nl:surfnet:diensten:institution-users', 0);
insert into virtual_resource_group (id, name, surf_connext_group_name, version) values (2, 'vrg2', 'urn:collab:group:test.surfteams.nl:nl:surfnet:diensten:institution-users2', 0);

insert into virtual_port (id, name, physical_port, version, virtual_resource_group) values (1, 'vp1-1', 1, 0, 1);
insert into virtual_port (id, name, physical_port, version, virtual_resource_group) values (2, 'vp2-1', 2, 0, 1);
insert into virtual_port (id, name, physical_port, version, virtual_resource_group) values (3, 'vp3-2', 3, 0, 2);
insert into virtual_port (id, name, physical_port, version, virtual_resource_group) values (4, 'vp4-2', 4, 0, 2);
