alter table orbeon_form_data       add xml_clob clob;
alter table orbeon_form_definition add xml_clob clob;

create or replace trigger orbeon_form_data_xml
         before insert on orbeon_form_data
for each row
begin
    if :new.xml_clob is not null then
        :new.xml      := XMLType(:new.xml_clob);
        :new.xml_clob := null;
    end if;
end;

create or replace trigger orbeon_form_definition_xml
         before insert on orbeon_form_definition
for each row
begin
    if :new.xml_clob is not null then
        :new.xml      := XMLType(:new.xml_clob);
        :new.xml_clob := null;
    end if;
end;
