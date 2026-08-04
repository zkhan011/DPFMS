#!/usr/bin/env python3
# SPDX-FileCopyrightText: DPW FMS Contributors
# SPDX-License-Identifier: MIT
import gzip,importlib.util,json,os,sqlite3,unittest
os.environ['FMS_OFFLINE_MBTILES_PATH']='deployment/maps/jebel-ali.mbtiles'
spec=importlib.util.spec_from_file_location('server','scripts/mbtiles_server.py');server=importlib.util.module_from_spec(spec);spec.loader.exec_module(server)
class Tests(unittest.TestCase):
 def test_xyz_to_tms(self):self.assertEqual(server.xyz_to_tms(14,7017),9366);self.assertRaises(ValueError,server.xyz_to_tms,3,8)
 def test_metadata(self):
  data=server.metadata();self.assertEqual(data['format'],'pbf');self.assertEqual(data['bounds'],'55.012,24.970,55.044,25.000');self.assertEqual(data['minzoom'],'12');self.assertEqual(data['maxzoom'],'16')
 def test_tile_and_missing(self):
  data=server.tile(14,10696,7017);self.assertEqual(data[:2],b'\x1f\x8b');self.assertTrue(gzip.decompress(data));self.assertIsNone(server.tile(14,0,0))
 def test_integrity_and_layers(self):
  with sqlite3.connect(os.environ['FMS_OFFLINE_MBTILES_PATH']) as db:
   self.assertEqual(db.execute('pragma integrity_check').fetchone()[0],'ok');layers={x['id'] for x in json.loads(dict(db.execute('select name,value from metadata'))['json'])['vector_layers']}
  self.assertTrue({'transportation','building','landuse','water','boundary','place'}<=layers)
if __name__=='__main__':unittest.main()
