package io.scalecube.services.benchmarks.transport.codec.protostuff;

import io.scalecube.services.benchmarks.transport.codec.SmCodecBenchmarkState;
import io.scalecube.services.benchmarks.transport.codec.SmPartialDecodeScenario;

public class ProtostuffSmPartialDecodeBenchmark {

  /**
   * Main method.
   *
   * @param args - params of main method.
   */
  public static void main(String[] args) {
    SmPartialDecodeScenario.runWith(args, SmCodecBenchmarkState.Protostuff::new);
  }
}
