import importlib.util
from pathlib import Path
import tempfile
import unittest


MODULE_PATH = Path(__file__).resolve().parents[1] / 'tools/research/nylocas_guidance_audit.py'
SPEC = importlib.util.spec_from_file_location('nylocas_ranger_guidance_identity', MODULE_PATH)
AUDIT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(AUDIT)


class RangerGuidanceIdentityTest(unittest.TestCase):
    def test_east_children_are_not_reported_as_missed_lane_rangers(self):
        # Reduced from native seed277700433457192: actual31 born264, children born267.
        def frame(tick, crabs):
            return f'    NYLO_TRACE\t{tick}\t0\t31\ttrue\tfalse\t3293,4248,0\tPIPE\t-1\tnull\tReserve next packet\t{crabs}\n'

        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            (directory / 'trials.csv').write_text('seed\n277700433457192\n')
            (directory / 'server.log').write_text(
                frame(264, '196,31,0,RANGE,false,8,3309,4248,0,0,false')
                + frame(267, '196,31,3,RANGE,false,8,3307,4248,0,0,false;199,31,0,MELEE,false,8,3306,4248,0,0,false')
                + frame(293, '199,31,26,MELEE,false,8,3301,4252,0,0,false'))
            rows, frames, launches = AUDIT.read_run(directory)
            self.assertEqual('east', frames[0]['crabs'][196]['lane'])
            self.assertEqual('split', frames[1]['crabs'][199]['lane'])
            result = AUDIT.ranger_audit(rows, frames, launches)
            self.assertEqual(0, result['wave31_alive_at_permanent_melee_rooms'])


if __name__ == '__main__':
    unittest.main()
