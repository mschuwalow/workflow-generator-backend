#!/usr/bin/env python3

from py4j.clientserver import JavaParameters, PythonParameters, ClientServer
import click
import logging
import sys

class Runner:

    def run_udf(self, code, arg):
        ns = {}
        exec(code, {}, ns)
        if "run" not in ns:
            raise ValueError("Function run was not defined")
        return ns["run"](arg)

    class Java:
        implements = ["app.udf.PythonRunner"]


@click.command()
@click.option('--port', type=int, help='port number')
def main(port: int):
    _start_logging()
    ClientServer(
        java_parameters=JavaParameters(eager_load = False, auto_convert = True, auto_field = True),
        python_parameters=PythonParameters(port=port, eager_load=True),
        python_server_entry_point=Runner()
    )

def _start_logging():
    logging.basicConfig(
        stream=sys.stdout,
        level=logging.INFO,
        format='%(levelname)s - %(name)s - %(message)s'
    )

if __name__ == '__main__':
    main()

