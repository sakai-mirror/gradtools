-----------------------------------------------------------------------------
-- DISSERTATION REALMS --
-----------------------------------------------------------------------------

INSERT INTO CHEF_REALM VALUES ('/dissertation/s/rackham', '<?xml version="1.0" encoding="UTF-8"?>
<realm id="/dissertation/s/rackham">
<ability auth="auth" lock="dis.step.read"/>
<ability auth="auth" lock="dis.step.upd"/>
<ability auth="auth" lock="dis.step.add"/>
<ability auth="auth" lock="dis.step.del"/>
</realm>
');

INSERT INTO CHEF_REALM VALUES ('/dissertation/d/rackham', '<?xml version="1.0" encoding="UTF-8"?>
<realm id="/dissertation/d/rackham">
<ability auth="auth" lock="dis.dis.upd"/>
</realm>
');

INSERT INTO CHEF_REALM VALUES ('/dissertation/g/rackham', '<?xml version="1.0" encoding="UTF-8"?>
<realm id="/dissertation/g/rackham">
<ability auth="auth" lock="dis.grp.upd"/>
</realm>
');

INSERT INTO CHEF_REALM VALUES ('/dissertation/p/rackham', '<?xml version="1.0" encoding="UTF-8"?>
<realm id="/dissertation/p/rackham">
<ability auth="auth" lock="dis.path.upd"/>
<ability auth="auth" lock="dis.path.del"/>
</realm>
');

INSERT INTO CHEF_REALM VALUES ('/dissertation/ss/rackham', '<?xml version="1.0" encoding="UTF-8"?>
<realm id="/dissertation/ss/rackham">
<ability auth="auth" lock="dis.status.upd"/>
<ability auth="auth" lock="dis.status.add"/>
<ability auth="auth" lock="dis.status.del"/>
<ability auth="auth" lock="dis.status.read"/>
</realm>
');

INSERT INTO CHEF_REALM VALUES ('/dissertation/i/rackham', '<?xml version="1.0" encoding="UTF-8"?>
<realm id="/dissertation/i/rackham">
<ability auth="auth" lock="dis.info.upd"/>
<ability auth="auth" lock="dis.info.add"/>
<ability auth="auth" lock="dis.info.del"/>
<ability auth="auth" lock="dis.info.read"/>
</realm>
');
