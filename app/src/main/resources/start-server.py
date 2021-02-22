#!/usr/bin/env python3

from py4j.clientserver import JavaParameters, PythonParameters, ClientServer
from py4j.java_gateway import GatewayClient, JavaClass, JavaGateway, is_instance_of, JavaObject
from py4j import protocol as proto
from py4j.protocol import (register_input_converter, register_output_converter)
import click
import logging
import sys
from datetime import date

DEFAULT_OUTPUT_CONVERTER = proto.OUTPUT_CONVERTER.copy()

def convert_reference_type(target_id, gateway_client: GatewayClient, java_gateway: JavaGateway):
    java_object = JavaObject(target_id, gateway_client)
    if is_instance_of(java_gateway, java_object, "java.time.LocalDate"):
        return date(java_object.getYear(), java_object.getMonthValue(), java_object.getDayOfMonth())
    else:
        return java_object

class DateTimeConverter:
    def can_convert(self, object):
        return isinstance(object, date)

    def convert(self, object, gateway_client):
        current_converter = proto.OUTPUT_CONVERTER
        proto.OUTPUT_CONVERTER = DEFAULT_OUTPUT_CONVERTER
        cls = JavaClass("java.time.LocalDate", gateway_client)
        result = cls.of(object.year, object.month, object.day)
        proto.OUTPUT_CONVERTER = current_converter
        return result

class Runner:
    def run_udf(self, code, arg):
        ns = {}
        exec(code, {}, ns)
        if "run" not in ns:
            raise ValueError("function run was not defined")
        return ns["run"](arg)

    class Java:
        implements = ["app.udf.PythonRunner"]

@click.command()
@click.option('--port', type=int, help='port number')
def main(port: int):
    initialize_logging()
    register_input_converter(DateTimeConverter())
    gateway = ClientServer(
        java_parameters=JavaParameters(eager_load = False, auto_convert = True, auto_field = True),
        python_parameters=PythonParameters(port=port, eager_load=True),
        python_server_entry_point=Runner()
    )
    register_output_converter(
        proto.REFERENCE_TYPE,
        lambda target_id, gateway_client: convert_reference_type(target_id, gateway_client, gateway))

def initialize_logging():
    logging.basicConfig(
        stream=sys.stdout,
        level=logging.INFO,
        format='%(levelname)s - %(name)s - %(message)s'
    )

if __name__ == '__main__':
    main()

