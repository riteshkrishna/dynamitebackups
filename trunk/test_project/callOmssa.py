import os
import csv

from django.template import loader, Context
from django.http import HttpResponse

def createOmssaCommand(request):

    inputParameterFile = 'InputFiles/omssa_inputFile.txt'
    omssaTemplateFile  = 'omssa_template.txt'

    parameter = {}

    # Read the input parameter file
    parameterFileReader = csv.reader(open(inputParameterFile,'rb'))
    for row in parameterFileReader:
        rowString = ','.join(row)
        splittedRow = rowString.split(',',1)
        parameter[splittedRow[0]] = splittedRow[1]

    print parameter

    print "\n " + omssaTemplateFile + "\n"

    # Create Omssa run command using the omssa template
    t = loader.get_template(omssaTemplateFile)
    c = Context(parameter)
    omssa_command = t.render(c)

    # Call the shell to execute command
    os.system(omssa_command)

    return HttpResponse(omssa_command)


