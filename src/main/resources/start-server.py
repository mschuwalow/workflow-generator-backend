#!/usr/bin/env python3

from py4j.clientserver import JavaParameters, PythonParameters, ClientServer
import click
import logging
import os
import signal
import sys

class Runner:

    def run_udf_1(self, code, arg):
        ns = {}
        exec(code, {}, ns)
        if "run" not in ns:
            raise ValueError("Function run was not defined")
        return ns["run"](arg)

    def run_udf_2(self, code, arg1, arg2):
        ns = {}
        exec(code, {}, ns)
        if "run" not in ns:
            raise ValueError("Function run was not defined")
        return ns["run"](arg1, arg2)

    class Java:
        implements = ["app.udf.PythonRunner"]


@click.command()
@click.option('--port', type=int, help='port number')
def main(port: int):
    _start_logging()
    ClientServer(
        java_parameters=JavaParameters(eager_load = False, auto_convert = True),
        python_parameters=PythonParameters(port=port, eager_load=True),
        python_server_entry_point=Runner()
    )

def _start_logging():
    logging.basicConfig(
        stream=sys.stdout,
        level=logging.INFO,
        format='%(levelname)s - %(name)s - %(message)s'
    )

class WaitLoop:
    kill_now = False

    def __init__(self):
        signal.signal(signal.SIGINT, self.exit_gracefully)
        signal.signal(signal.SIGTERM, self.exit_gracefully)

    def exit_gracefully(self, signum, frame):
        self.kill_now = True

    def run(self):
        while not self.kill_now:
            time.sleep(1)


if __name__ == '__main__':
    main()

