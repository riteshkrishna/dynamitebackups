package GetFDRValues;
require Exporter;

our @ISA = qw(Exporter);
our $VERSION = 1.0;;
our @EXPORT = qw(GetFDRValues);

use URI::Escape;

#use lib qw(C:/Work/MultipleSearch/FDRWeb/perl_modules/);
#use MascotParser;



#####################################################################
##   Jennifer Siepen  2008                                         ##
## Returns the score threshold for different FDR values for either ##
## the gygi of Kall FDR methods.                                   ##
#####################################################################





sub GetFDRValues
{
my $fdrtype = shift;
my $se = shift;
my @results = @_;

my @graph;
my %fdr_all;
my $min = 1;
my $max = 100;

my %score_count;
my %nter_count;
my @scorerecord;
my $identity_count = 0;
my $identity;
my %unique_peptides;

my $expect_count = 0;
my %expect_peptides;


 #for all the peptides
# foreach my $pep (keys %{$results[0]})
 for(my $s=0 ; $s<scalar(@results) ; $s++)
 {
  for(my $r=1 ; $r<11 ; $r++)
  {
   if($results[$s][$r]{'sequence'} && $results[$s][$r]{'sequence'} ne "NULL")
   {
   my $pep = $results[$s][$r]{'sequence'};
#   if($se ne "O")
#   {
#   $disc = int($results[$s][$r]{'ionscore'});
#   }
#   else
#   {
#   $disc = sprintf("%.5f",$results[$s][$r]{'ionscore'});
#   }
$disc = $results[$s][$r]{'expect'};
    #get the sum of for and negative hits above this score
    if(!$fdr_all{$disc})
    {
    push(@scorerecord,$disc);
    my @sum = GetSums($disc,$se,@results);

    #forward is sum[0] and reverse sum[1]
     if($fdrtype == '0')
     {
     #forward is sum[0] and reverse sum[1]
      if($sum[1] == 0)
      {
      $fdr= 0;
      $fdr_all{$disc} = $fdr;
      }
      else
      {
      $fdr = ($sum[1])+($sum[0]);
      $fdr = (2*$sum[1])/$fdr;
      $fdr_all{$disc} = $fdr;
      }
     }
     else
     {
      if($sum[1] == 0)
      {
      $fdr = 0;
      $fdr_all{$disc} = $fdr;
      }
      else
      {
      $fdr = $sum[0];
      $fdr = $sum[1]/$fdr;
      $fdr_all{$disc} = $fdr;
      }
     }
    }

    if($score_count{$disc})
    {
    $score_count{$disc}++;
    }
    else
    {
    $score_count{$disc} = 1;
    }
    if($results[$s][$r]{'start'}<3 && $nter_count{$disc})
    {
    $nter_count{$disc}++;
    }
    elsif($results[$s][$r]{'start'}<3)
    {
    $nter_count{$disc} = 1;
    }
   }
  }
 }

my %counter;
my %ntercounter;
my $fdr;

my %scorethresholds;

 #for a set of different FDR levels
 for($fdr=0.01 ; $fdr<0.50 ; $fdr = $fdr+0.05)
 {
 $counter{$fdr} = 0;
 $ntercounter{$fdr} = 0;
 my $min_score = -1;
  foreach my $score (keys %fdr_all)
  {
   #if the fdr at this score is better than threshold
   if($fdr_all{$score} <= $fdr)
   {
    if($score>$min_score)
    {
    $min_score = $score;
    }
   $counter{$fdr} += $score_count{$score};
   $ntercounter{$fdr} += $nter_count{$score}; 
   }
  }
 $scorethresholds{$fdr} = $min_score;
 }

 my %res;
 #for all the fdr scores
 foreach my $fdr (keys %counter)
 {
 $res{$fdr}[0] = $counter{$fdr};
 $res{$fdr}[1] = $ntercounter{$fdr};
 }


return %scorethresholds;
} 


sub InitColors {
    my($im) = $_[0];
    # Allocate colors
    $white = $im->colorAllocate(255,255,255);
    $black = $im->colorAllocate(0,0,0);
    $red = $im->colorAllocate(255,0,0);
    $blue = $im->colorAllocate(0,0,255);
    $green = $im->colorAllocate(0, 255, 0);

    $brown = $im->colorAllocate(255, 0x99, 0);
    $violet = $im->colorAllocate(255, 0, 255);
    $yellow = $im->colorAllocate(255, 255, 0);
}

sub CountSpectra
{
my $threshold = shift;
my %counts = %{$_[0]};
my $result = 0;

 #for all the results
 foreach my $score (keys %counts)
 {
  if($score <= $threshold)
  {
  $result += $counts{$score};
  }
 }

return $result;
}


return 1;


sub GetSums
{
my $threshold = shift;
my $scoretype = shift;
my @results = @_;
my @sum;


#forward
$sum[0] = 0;
#reverse
$sum[1] = 0;
my $disc;

 #forward first
 for(my $s=0 ; $s<scalar(@results) ; $s++)
 {
 my $r = 1;
  if($results[$s][$r]{'sequence'} && $results[$s][$r]{'sequence'} ne "NULL" && $results[$s][$r]{'protein'} !~ m/^REV\_/)
  {
#   if($scoretype ne "O")
#   {
#   $disc = int($results[$s][$r]{'ionscore'});
#   }
#   else
#   {
#   $disc = sprintf("%.5f",$results[$s][$r]{'ionscore'});
#   }
$disc = $results[$s][$r]{'expect'};

   if($disc <= $threshold)
   {
   $sum[0]++;
   }
  }
  elsif($results[$s][$r]{'sequence'} && $results[$s][$r]{'sequence'} ne "NULL")
  {
#   if($setype ne "O")
#   {
#   $disc = int($results[$s][$r]{'ionscore'});
#   }
#   else
#   {
#   $disc = sprintf("%.5f",$results[$s][$r]{'ionscore'});
#   }
$disc = $results[$s][$r]{'expect'};
   if($disc <= $threshold)
   {
   $sum[1]++;
   }
  }
 }


return @sum;
}



 
sub GetDbPath
{
my $file = shift;
my $db;
open(FILE,"<$file") or die "unable to open the mascot results, $file\n";
 while(my $line = <FILE>)
 {
  if($line =~ m /^fastafile\=/)
  {
  my @split = split/\=/,$line;
  $db = $split[1];
  close FILE;
  }  
 }
close FILE;

$db =~ s/\/usr\/local\/mascot\//\/fs\/msct\//;


return $db;

}

sub GetIdentity
{
my $qmatch = shift;
my $id = 0;

 if($qmatch<1)
 {
 $id = -10.0 * log(1.0/10.0)/log(10);
 }
 else
 {
 $id = -10 * log(1.0/(1.0*$qmatch))/log(10);
 }

return $id;

}
