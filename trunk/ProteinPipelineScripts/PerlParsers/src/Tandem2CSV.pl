#!/usr/bin/perl

use strict;
use XML::Simple qw(:strict);
use Data::Dumper;

if(@ARGV != 2){
	print "Usage: Tandem2CSV.pl [inputfile].xml outputfile.csv\n";
	exit(1);
}

my $tandem_result = $ARGV[0];
my $outfile = $ARGV[1];
open(CSV,">$outfile") or die "unable to open the $outfile\n";

our @columns = ("Spectrum number", "Filename/id", "Peptide", "E-value", "Mass", "GI" , "Accession", "Start", "Stop", "Defline", "Mods", "Charge", "Theo Mass", "Tandem Hyperscore" );
our $printHeaderLine = 1;

ReadTandemXML($tandem_result);

sub ReadTandemXML(){

	my $xml_file = shift;
	
	my @pep_results;
	
	# create object
	my $xml = new XML::Simple(ForceArray => [ 'group','protein','domain','peptide' ] , KeyAttr => 'NOTUSED');
	# read XML file
	
	print("XML object created\n");

	my $data;
	my $res = eval{$data = $xml->XMLin($xml_file)};
	if(! $res)
	{
		print("error extracting data from XML object\n");
		warn $@;
		return 0;
	}

	print("data extracted from XML object\n");

	my %all_data = %{$data};
	
	my @all_records = @{$all_data{'group'}};
	%all_data = ();
	
	my @csvResults;

	#print Dumper(%all_data);
	#print Dumper(@all_records);


	for(my $i=0 ; $i<@all_records ; $i++){
	
		my %record = %{$all_records[$i]};		
		
		if($record{'protein'}){
			
			my @proteins = @{$record{'protein'}};			
			
			my $mh = $record{'mh'};
			my $maxI = $record{'maxI'};
			
			#Omssa CSV is: Spectrum number, Filename/id, Peptide, E-value, Mass, gi, Accession, Start, Stop, Defline, Mods, Charge, Theo Mass, P-value, NIST score
			my ($prot_acc,$specName,$specID,$pepSeq,$eval,$mass,$gi,$acc,$start,$end,$defline,$mods,$charge,$theoMass);
			
			my @groups = @{$record{'group'}};			
			my %group;
			
			foreach my $tmp_group (@groups){
				my %temp = %{$tmp_group};
				my $label = $temp{'label'};
				if($label eq "fragment ion mass spectrum"){
					%group = %temp;
				}				
			}
			
			if(%group){
				my %gamlTrace = %{$group{'GAML:trace'}};				
				my %note = %{$group{'note'}};
				
				if(%gamlTrace){
					$specID = $gamlTrace{'id'};
					my @gamlAtt = @{$gamlTrace{'GAML:attribute'}};
					
					if(@gamlAtt){
						foreach my $att (@gamlAtt){
							my %att = %{$att};
							if($att{'type'} eq "M+H"){
								$mass = $att{'content'}
							}
							if($att{'type'} eq "charge"){
								$charge = $att{'content'}
							}
						}
					}
					else{
						print "no GAML attribute\n";
						print Dumper(%gamlTrace);
						exit;
					}
				}
				else{
					print "no GAML trace\n";
					print Dumper(%group);
					exit;
				}
				
				if(%note){
					$specName = $note{'content'};
					$specName =~ s/\n|\r//g;		#remove any EOL chars - adapted to make it cross-platform (DCW 140410)
				}
				else{
					print "no note (spectrum identifier)\n";
					print Dumper(%group);
					exit; 
				}
			}
			else{
				"no group when mh is $mh \n";
				print Dumper(%record);
				exit;
			}

			foreach my $prot (@proteins){			
				my %protein = %{$prot};
				my @results = @{getProteinDetails(\%protein)};

				for(my $j=0;$j<@results;$j++){
					my %res = %{$results[$j]};
					$res{'Spectrum number'} = $specID - 1;	#Tandem starts counting from 1, Omssa from zero - reset tandem to zero;
					$res{'Filename/id'} = $specName;
					$res{'Mass'} = $mass;
					$res{'Charge'} = $charge;
					push(@pep_results,\%res);
				}				
			}


		}
	}
	printCSV(\@pep_results);
}


sub getProteinDetails{
	
	my $tmp = shift;
	my %protein = %{$tmp};
	
	my @results;
	
	#$res{'Accession'} = $protein{'label'};
	my $prot_acc = $protein{'label'};
	
	if($protein{'peptide'}){
		
		my @peptides = @{$protein{'peptide'}};			
		if(@peptides > 1){
			die "Fatal error, unexpected array of peptide elements reported\n";
		}
		
		my %peptide = %{$peptides[0]};		
		
		#my %peptide = %{$protein{'peptide'}};
		
		if(%peptide){
			my @domains = @{$peptide{'domain'}};		
			
			foreach my $domain (@domains){
				my %domain = %{$domain};						
				my %res = %{getOneResult(\%domain)};
				$res{'Accession'} = $protein{'label'};

				#DCW (140410)
				if($res{'Accession'} =~ /,/)
				{
					$res{'Accession'} = "\"".$res{'Accession'} ."\"";
				}
					
				push(@results,\%res);
			}			
		}
	}
	else{
		print "no peptides for %protein\n";
		print Dumper(%protein);
		exit;
	}

	return \@results;
}

sub getOneResult{

	my $tmp = shift;
	my %domain = %{$tmp};
	my %res;
	$res{'Peptide'} = $domain{'seq'};
	$res{'E-value'} = $domain{'expect'};
	$res{'Start'} = $domain{'start'};
	$res{'Stop'}= $domain{'end'};			
	$res{'Theo Mass'}	= $domain{'mh'};
	$res{'Tandem Hyperscore'} = $domain{'hyperscore'};						

	my $mod_element = $domain{'aa'};
	
	if($mod_element){
		$res{'Mods'} = getModString($mod_element,$domain{'start'});
	}
	return \%res;
}


sub getModString{

	my $mod_element = shift;
	my $start = shift;
	
	my $mod_string;
	
	#Don't know whether $mod_element is an array or a hash	
	if(ref($mod_element) eq 'ARRAY'){
		$mod_string = "\"";
		foreach my $tmp_mod (@{$mod_element}){
			my %mod = %{$tmp_mod};
			
			my $pos = $mod{'at'} - $start + 1; 
			$mod_string .= ",".$mod{'modified'} . "_" . $mod{'type'} . ":". $pos;
		}
		$mod_string =~ s/^\"\,/\"/;	#remove leading ,
		$mod_string .= "\"";
	}
	else{
		my %mod = %{$mod_element};
		my $pos = $mod{'at'} - $start +1 ; 		
		$mod_string = $mod{'modified'} . "_" . $mod{'type'} . ":". $pos;
	}
		
	return $mod_string;
}

sub printCSV{

	my $tmp = shift;
	my @results = @{$tmp};
	
	if($printHeaderLine){
		my $line;
		for(my $j = 0; $j < @columns; $j++){
			$line .=  "," . $columns[$j];
		}
		$line =~ s/^\,//;	#remove leading ,
		print CSV "$line\n"
	}
	
	for(my $i = 0; $i < @results; $i++){

		my %res = %{$results[$i]};
		my $line;
		for(my $j = 0; $j < @columns; $j++){
			$line .= "," . $res{$columns[$j]};
		}
		$line =~ s/^\,//;	#remove leading ,
		print CSV "$line\n";
					
	}
}
