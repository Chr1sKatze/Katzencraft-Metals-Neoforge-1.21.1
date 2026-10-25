import unittest
from summarize_foundry_jfr import PREFIX, summarize


def frame(name):
    owner, method = name.rsplit('.', 1)
    return {'method': {'type': {'name': owner.replace('.', '/')}, 'name': method}}


class SummaryChecks(unittest.TestCase):
    def test_inclusive_deduplication_and_thread_separation(self):
        method = PREFIX + 'client.renderer.FoundryControllerTankLiquidRenderer.render'
        events = [dict(type='jdk.ExecutionSample', values={
            'sampledThread': {'javaName': thread},
            'stackTrace': {'frames': [frame(method), frame(method)]}})
            for thread in ('Render thread', 'Server thread')]
        text = summarize(events)
        self.assertEqual(text.count('(100.00%)'), 2)
        self.assertIn('Thread Server thread: 1', text)
        self.assertIn('Thread Render thread: 1', text)

    def test_allocation_nearest_caller_and_missing_stack(self):
        method = PREFIX + 'client.sound.FoundryTankSoundManager.tick'
        text = summarize([
            {'type': 'jdk.ObjectAllocationSample', 'values': {'weight': 4096,
             'stackTrace': {'frames': [frame('java.util.ArrayList.grow'), frame(method)]}}},
            {'type': 'jdk.ExecutionSample', 'values': {'sampledThread': {'javaName': 'Render thread'}}}])
        self.assertIn('4,096 estimated bytes: ' + method, text)
        self.assertIn('No Katzencraft frames sampled', text)
        self.assertNotIn('100.00%', text)

    def test_native_samples_and_empty_recording(self):
        text = summarize([{'type': 'jdk.NativeMethodSample', 'values': {
            'sampledThread': {'javaName': 'Render thread'},
            'stackTrace': {'frames': [frame('org.lwjgl.opengl.GL11.glDrawArrays')]}}}])
        self.assertIn('Render thread: 1', text)
        self.assertIn('No execution/native samples found', summarize([]))


if __name__ == '__main__':
    unittest.main()
