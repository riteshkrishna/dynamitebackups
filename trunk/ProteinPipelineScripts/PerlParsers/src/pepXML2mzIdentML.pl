#!/usr/bin/perl

use strict;


if(@ARGV != 4){
	print "Usage: pepXML2mzIdentML [results].xml [params].csv [outputfile].mzid [score-types]\n";
	print "score-types = comma separated list of the named scores in the file e.g \"pepxml2CSV.pl expect,ionscore\" or \"pepxml2CSV.pl xcorr,deltacn,spscore\"\n";
	exit(1);
}
my $results_file = $ARGV[0];
my $paramFile = $ARGV[1];
my $outfile = $ARGV[2];
my $score_type = $ARGV[3];

my $comm = "pepxml2CSV.pl $results_file temp.csv $score_type";

system($comm);
$comm = "csv2mzIdentML.pl temp.csv $paramFile $outfile";
system($comm);
$comm = "del temp.csv\n";
system($comm);


