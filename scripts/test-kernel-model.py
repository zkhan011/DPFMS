#!/usr/bin/env python3
# SPDX-FileCopyrightText: DPW FMS Contributors
# SPDX-License-Identifier: MIT
import json,unittest
from pathlib import Path
MODEL=Path('opentcs-web-ui/src/main/resources/kernel-model/dpw-fms-plant-model.json')
class Tests(unittest.TestCase):
 @classmethod
 def setUpClass(cls):cls.model=json.loads(MODEL.read_text())
 def test_stable_identity_and_assets(self):
  self.assertEqual(self.model['name'],'DPW-FMS-JEBEL-ALI-V1');self.assertEqual(len(self.model['points']),36);self.assertEqual(len(self.model['paths']),70);self.assertEqual(len(self.model['locations']),19);self.assertEqual(len(self.model['vehicles']),20)
 def test_routable_graph_and_links(self):
  names={x['name'] for x in self.model['points']};edges={(x['srcPointName'],x['destPointName']) for x in self.model['paths']}
  self.assertTrue(all(a in names and b in names for a,b in edges));self.assertTrue(all((b,a) in edges for a,b in edges));self.assertTrue(all(x['links'][0]['pointName'] in names and 'MOVE' in x['links'][0]['allowedOperations'] for x in self.model['locations']))
 def test_geo_properties_and_dispatch_operations(self):
  self.assertTrue(all({p['name'] for p in x['properties']} >= {'latitude','longitude'} for x in self.model['points']));self.assertIn('MOVE',self.model['locationTypes'][0]['allowedOperations'])
if __name__=='__main__':unittest.main()
