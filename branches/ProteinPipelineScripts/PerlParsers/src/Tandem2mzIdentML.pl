#!/usr/bin/perl

use strict;
use FindBin;
use lib "$FindBin::Bin";

if(@ARGV != 3){
	print "Usage: Tandem2mzIdentML [results].xml [params].csv [outputfile].mzid\n";
	exit(1);
}
my $results_file = $ARGV[0];
#my $paramFile = $FindBin::Bin."/".$ARGV[1];
#my $outfile = $FindBin::Bin."/".$ARGV[2];

my $paramFile = $ARGV[1];
my $outfile = $ARGV[2];

#my $comm = "Tandem2CSV.pl $results_file temp.csv";
#system($comm);
#$comm = "csv2mzIdentML.pl temp.csv $paramFile $outfile";
#system($comm);
#$comm = "del temp.csv\n";
#system($comm);


#my $tempFile = "/var/www/tmp/temp.csv";
my $tempFile = $outfile."TEMP";

my $comm = "perl ".$FindBin::Bin."/Tandem2CSV.pl \"$results_file\" \"$tempFile\"";


system($comm);
$comm = "perl ".$FindBin::Bin."/csv2mzIdentML.pl \"$tempFile\" \"$paramFile\" \"$outfile\"";
system($comm);

my $os = $^O;
#print "Running in: $os\n";

if($os eq "MSWin32"){
	$tempFile =~ s/\//\\/g;	#replace "/" with "\"
	$comm = "del $tempFile\n";	
}
else{
	$comm = "rm $tempFile\n";
}

print "$comm\n";
system($comm);