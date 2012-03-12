package PeptideHit;


use strict;

#constructor
sub new {
    my ($class) = @_;
	
    my $self = {
	    

		_evalue => undef,		
		_pvalue => undef,
		_sequence => undef,
		_delta => undef,
		_spec_title => undef,
		_charge => undef,
		_pag => undef,
		_prot_score => undef,
		_prec_masstocharge => undef,
		_query_index => undef,		#the index of the spectrum, this has been changed so these are arbitrary numbers - cannot be calculated for Mascot to correspond with index pos
		_theo_mass => undef,
		_start => undef,			#start of peptide within protein sequence
		_end => undef,				#end of peptide within protein sequence
		_id => undef,				#arbitrary ID for pep_hit so it can be located later
		_mods => undef,
		_protein_map => []
		

    };
	bless $self, $class;
    return $self;	
}

sub queryIndex {
    my ( $self, $q ) = @_;
    $self->{_query_index} = $q if defined($q);
    return $self->{_query_index};
}

sub evalue {
    my ( $self, $e ) = @_;
    $self->{_evalue} = $e if defined($e);
    return $self->{_evalue};
}


sub pvalue {
    my ( $self, $p ) = @_;
    $self->{_pvalue} = $p if defined($p);
    return $self->{_pvalue};
}


sub sequence {
    my ( $self, $seq ) = @_;
    $self->{_sequence} = $seq if defined($seq);
    return $self->{_sequence};
}

sub delta {
    my ( $self, $d ) = @_;
    $self->{_delta} = $d if defined($d);
    return $self->{_delta};
}

sub pag {
    my ( $self, $p ) = @_;
    $self->{_pag} = $p if defined($p);
    return $self->{_pag};
}

sub protScore {
    my ( $self, $p ) = @_;
    $self->{_prot_score} = $p if defined($p);
    return $self->{_prot_score};
}

sub specTitle {
    my ( $self, $t ) = @_;
    $self->{_spec_title} = $t if defined($t);
    return $self->{_spec_title};
}

sub charge {
    my ( $self, $z ) = @_;
    $self->{_charge} = $z if defined($z);
    return $self->{_charge};
}

sub precMassToCharge{
	my ( $self, $mz ) = @_;
    $self->{_prec_masstocharge} = $mz if defined($mz);
    return $self->{_prec_masstocharge};
}

sub theoMass {
    my ( $self, $m ) = @_;
    $self->{_theo_mass} = $m if defined($m);
    return $self->{_theo_mass};
}

sub mods {
    my ( $self, $m ) = @_;
    $self->{_mods} = $m if defined($m);
    return $self->{_mods};
}


sub addProteinMap{
    my ( $self, $protAcc ) = @_;
	push ( @{ $self->{_protein_map} } ,$protAcc);		
}

sub getProteinMaps{
	my ($self) = @_;
	return @{$self->{_protein_map}};
}


sub start {
    my ( $self, $s ) = @_;
    $self->{_start} = $s if defined($s);
    return $self->{_start};
}

sub end {
    my ( $self, $e ) = @_;
    $self->{_end} = $e if defined($e);
    return $self->{_end};
}

sub pepHitID {
    my ( $self, $i ) = @_;
    $self->{_id} = $i if defined($i);
    return $self->{_id};
}



1;
