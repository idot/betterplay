import { EvenoddPipe } from './evenodd.pipe';

describe('EvenoddPipe', () => {
  it('create an instance', () => {
    const pipe = new EvenoddPipe();
    expect(pipe).toBeTruthy();
  });
});
